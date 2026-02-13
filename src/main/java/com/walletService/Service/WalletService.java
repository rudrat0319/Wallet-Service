package com.walletService.Service;

import com.walletService.Dto.BalanceResponseDto;
import com.walletService.Dto.WalletOperationRequestDto;
import com.walletService.Dto.WalletOperationResponseDto;
import com.walletService.Entity.*;
import com.walletService.Exceptions.*;
import com.walletService.Repository.IdempotencyKeyRepository;
import com.walletService.Repository.LedgerEntryRepository;
import com.walletService.Repository.UserRepository;
import com.walletService.Repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.0001");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999999.9999");


    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
    public WalletOperationResponseDto topUp(Long userId, WalletOperationRequestDto request) {
        log.info("Processing top-up for user {} with idempotency key {}", userId, request.getIdempotencyKey());

        Optional<WalletOperationResponseDto> cachedResponse = checkIdempotency(userId, request.getIdempotencyKey());
        if (cachedResponse.isPresent()) {
            log.info("Returning cached response for duplicate request");
            return cachedResponse.get();
        }

        validateAmount(request.getAmount());

        User user = validateUser(userId);

        Wallet wallet = lockWallet(userId, request.getAssetType());

        LedgerEntry ledgerEntry = createLedgerEntry(
                wallet,
                TransactionType.CREDIT,
                request.getAmount(),
                request.getDescription() != null ? request.getDescription() : "Wallet top-up",
                request.getReferenceId(),
                request.getIdempotencyKey()
        );

        wallet.setBalance(wallet.getBalance().add(request.getAmount()));
        walletRepository.save(wallet);

        WalletOperationResponseDto response = buildResponse(ledgerEntry, "Top-up successful");

        saveIdempotencyKey(userId, request.getIdempotencyKey(), ledgerEntry.getId(), response);

        log.info("Top-up completed successfully for user {}", userId);
        return response;
    }


    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
    public WalletOperationResponseDto grantIncentive(Long userId, WalletOperationRequestDto request) {
        log.info("Processing incentive grant for user {} with idempotency key {}",
                userId, request.getIdempotencyKey());

        Optional<WalletOperationResponseDto> cachedResponse = checkIdempotency(userId, request.getIdempotencyKey());
        if (cachedResponse.isPresent()) {
            log.info("Returning cached response for duplicate incentive request");
            return cachedResponse.get();
        }

        validateAmount(request.getAmount());

        User user = validateUser(userId);

        Wallet wallet = lockWallet(userId, request.getAssetType());

        LedgerEntry ledgerEntry = createLedgerEntry(
                wallet,
                TransactionType.CREDIT,
                request.getAmount(),
                request.getDescription() != null ? request.getDescription() : "Bonus/Incentive credit",
                request.getReferenceId(),
                request.getIdempotencyKey()
        );

        wallet.setBalance(wallet.getBalance().add(request.getAmount()));
        walletRepository.save(wallet);

        WalletOperationResponseDto response = buildResponse(ledgerEntry, "Incentive granted successfully");

        saveIdempotencyKey(userId, request.getIdempotencyKey(), ledgerEntry.getId(), response);

        log.info("Incentive granted successfully for user {}", userId);
        return response;
    }


    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
    public WalletOperationResponseDto spend(Long userId, WalletOperationRequestDto request) {
        log.info("Processing spend for user {} with idempotency key {}", userId, request.getIdempotencyKey());

        Optional<WalletOperationResponseDto> cachedResponse = checkIdempotency(userId, request.getIdempotencyKey());
        if (cachedResponse.isPresent()) {
            log.info("Returning cached response for duplicate spend request");
            return cachedResponse.get();
        }

        validateAmount(request.getAmount());

        User user = validateUser(userId);

        Wallet wallet = lockWallet(userId, request.getAssetType());

        validateSufficientBalance(wallet, request.getAmount());

        LedgerEntry ledgerEntry = createLedgerEntry(
                wallet,
                TransactionType.DEBIT,
                request.getAmount(),
                request.getDescription() != null ? request.getDescription() : "Currency spend",
                request.getReferenceId(),
                request.getIdempotencyKey()
        );

        wallet.setBalance(wallet.getBalance().subtract(request.getAmount()));
        walletRepository.save(wallet);

        WalletOperationResponseDto response = buildResponse(ledgerEntry, "Spend successful");

        saveIdempotencyKey(userId, request.getIdempotencyKey(), ledgerEntry.getId(), response);

        log.info("Spend completed successfully for user {}", userId);
        return response;
    }


    @Transactional(readOnly = true)
    public BalanceResponseDto getBalance(Long userId, AssetType assetType) {
        log.info("Fetching balance for user {} and asset type {}", userId, assetType);

        // Validate user
        validateUser(userId);

        // Get wallet without lock (read-only)
        Wallet wallet = walletRepository.findByUserIdAndAssetType(userId, assetType)
                .orElseThrow(() -> new WalletNotFoundException(
                        String.format("Wallet not found for user %d and asset type %s", userId, assetType)
                ));

        return BalanceResponseDto.builder()
                .userId(userId)
                .assetType(assetType)
                .balance(wallet.getBalance())
                .build();
    }


    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidTransactionAmountException("Amount cannot be null");
        }
        if (amount.compareTo(MIN_AMOUNT) < 0) {
            throw new InvalidTransactionAmountException(
                    String.format("Amount must be at least %s", MIN_AMOUNT)
            );
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidTransactionAmountException(
                    String.format("Amount cannot exceed %s", MAX_AMOUNT)
            );
        }
    }


    private Optional<WalletOperationResponseDto> checkIdempotency(Long userId, String idempotencyKey) {
        Optional<IdempotencyKey> existing = idempotencyKeyRepository.findByKeyAndUserId(idempotencyKey, userId);

        if (existing.isPresent()) {
            IdempotencyKey key = existing.get();

            if (key.getExpiresAt().isBefore(LocalDateTime.now())) {
                return Optional.empty();
            }

            try {
                WalletOperationResponseDto response = objectMapper.readValue(
                        key.getResponseData(),
                        WalletOperationResponseDto.class
                );
                return Optional.of(response);
            } catch (Exception e) {
                log.error("Error deserializing cached response", e);
                throw new TransactionProcessingException("Error retrieving cached response", e);
            }
        }

        return Optional.empty();
    }


    private Wallet lockWallet(Long userId, AssetType assetType) {
        Optional<Wallet> walletOpt = walletRepository.findByUserIdAndAssetTypeForUpdate(userId, assetType);

        if (walletOpt.isPresent()) {
            return walletOpt.get();
        }


        User user = userRepository.findById(userId)
                .orElseThrow(() -> new WalletNotFoundException("User not found: " + userId));

        Wallet newWallet = Wallet.builder()
                .user(user)
                .assetType(assetType)
                .balance(BigDecimal.ZERO)
                .build();

        Wallet savedWallet = walletRepository.save(newWallet);

        return walletRepository.findByUserIdAndAssetTypeForUpdate(userId, assetType)
                .orElseThrow(() -> new WalletNotFoundException("Failed to create and lock wallet"));
    }


    private User validateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new WalletNotFoundException("User not found: " + userId));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedWalletAccessException(
                    String.format("User account is %s. Wallet operations are not allowed.", user.getStatus())
            );
        }

        return user;
    }


    private void validateSufficientBalance(Wallet wallet, BigDecimal amount) {
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    String.format("Insufficient balance. Available: %s, Required: %s",
                            wallet.getBalance(), amount)
            );
        }
    }


    private LedgerEntry createLedgerEntry(Wallet wallet, TransactionType type, BigDecimal amount,
                                          String description, String referenceId, String idempotencyKey) {
        BigDecimal newBalance = type == TransactionType.CREDIT
                ? wallet.getBalance().add(amount)
                : wallet.getBalance().subtract(amount);

        LedgerEntry entry = LedgerEntry.builder()
                .wallet(wallet)
                .transactionType(type)
                .amount(amount)
                .balanceAfter(newBalance)
                .description(description)
                .referenceId(referenceId)
                .idempotencyKey(idempotencyKey)
                .build();

        return ledgerEntryRepository.save(entry);
    }


    private WalletOperationResponseDto buildResponse(LedgerEntry entry, String message) {
        return WalletOperationResponseDto.builder()
                .transactionId(entry.getId())
                .transactionType(entry.getTransactionType())
                .amount(entry.getAmount())
                .balanceAfter(entry.getBalanceAfter())
                .assetType(entry.getWallet().getAssetType())
                .description(entry.getDescription())
                .referenceId(entry.getReferenceId())
                .timestamp(entry.getCreatedAt())
                .message(message)
                .build();
    }


    private void saveIdempotencyKey(Long userId, String key, Long ledgerEntryId,
                                    WalletOperationResponseDto response) {
        try {
            String responseData = objectMapper.writeValueAsString(response);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new WalletNotFoundException("User not found: " + userId));

            IdempotencyKey idempotencyKey = IdempotencyKey.builder()
                    .key(key)
                    .user(user)
                    .ledgerEntryId(ledgerEntryId)
                    .responseData(responseData)
                    .build();

            idempotencyKeyRepository.save(idempotencyKey);
        } catch (Exception e) {
            log.error("Error serializing response for idempotency key", e);
            throw new TransactionProcessingException(
                    "Failed to serialize idempotency response", e
            );
        }
    }
}
