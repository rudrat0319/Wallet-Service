package com.walletService.Service;

import com.walletService.Dto.TransactionHistoryResponseDto;
import com.walletService.Entity.AssetType;
import com.walletService.Entity.LedgerEntry;
import com.walletService.Entity.Wallet;
import com.walletService.Exceptions.WalletNotFoundException;
import com.walletService.Repository.LedgerEntryRepository;
import com.walletService.Repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionHistoryService {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;


    @Transactional(readOnly = true)
    public TransactionHistoryResponseDto getTransactionHistory(Long userId, AssetType assetType,
                                                               LocalDateTime fromTime, LocalDateTime toTime) {
        log.info("Fetching transaction history for user {} and asset type {}", userId, assetType);

        // Get wallet
        Wallet wallet = walletRepository.findByUserIdAndAssetType(userId, assetType)
                .orElseThrow(() -> new WalletNotFoundException(
                        String.format("Wallet not found for user %d and asset type %s", userId, assetType)
                ));

        // Get ledger entries based on time filter
        List<LedgerEntry> ledgerEntries;

        if (fromTime != null && toTime != null) {
            ledgerEntries = ledgerEntryRepository.findByWalletIdAndCreatedAtBetween(
                    wallet.getId(), fromTime, toTime
            );
        } else {
            ledgerEntries = ledgerEntryRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());
        }

        // Convert to DTOs
        List<TransactionHistoryResponseDto.TransactionDto> transactions = ledgerEntries.stream()
                .map(this::convertToTransactionDto)
                .collect(Collectors.toList());

        return TransactionHistoryResponseDto.builder()
                .assetType(assetType)
                .currentBalance(wallet.getBalance())
                .transactions(transactions)
                .build();
    }

    /**
     * Get recent transactions (last N transactions)
     */
    @Transactional(readOnly = true)
    public TransactionHistoryResponseDto getRecentTransactions(Long userId, AssetType assetType, int limit) {
        log.info("Fetching last {} transactions for user {} and asset type {}", limit, userId, assetType);

        Wallet wallet = walletRepository.findByUserIdAndAssetType(userId, assetType)
                .orElseThrow(() -> new WalletNotFoundException(
                        String.format("Wallet not found for user %d and asset type %s", userId, assetType)
                ));

        List<LedgerEntry> ledgerEntries = ledgerEntryRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());

        // Limit results
        List<TransactionHistoryResponseDto.TransactionDto> transactions = ledgerEntries.stream()
                .limit(limit)
                .map(this::convertToTransactionDto)
                .collect(Collectors.toList());

        return TransactionHistoryResponseDto.builder()
                .assetType(assetType)
                .currentBalance(wallet.getBalance())
                .transactions(transactions)
                .build();
    }


    private TransactionHistoryResponseDto.TransactionDto convertToTransactionDto(LedgerEntry entry) {
        return TransactionHistoryResponseDto.TransactionDto.builder()
                .id(entry.getId())
                .type(entry.getTransactionType())
                .amount(entry.getAmount())
                .balanceAfter(entry.getBalanceAfter())
                .description(entry.getDescription())
                .referenceId(entry.getReferenceId())
                .timestamp(entry.getCreatedAt())
                .build();
    }
}
