package com.walletService.Controller;

import com.walletService.Dto.BalanceResponseDto;
import com.walletService.Dto.WalletOperationRequestDto;
import com.walletService.Dto.WalletOperationResponseDto;
import com.walletService.Entity.AssetType;
import com.walletService.Service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Slf4j
public class WalletController {

    private final WalletService walletService;


    @PostMapping("/topup")
    public ResponseEntity<WalletOperationResponseDto> topUpWallet(
            @Valid @RequestBody WalletOperationRequestDto request,
            Authentication authentication) {

        Long userId = getUserIdFromAuth(authentication);
        log.info("Top-up request received for user {}", userId);

        WalletOperationResponseDto response = walletService.topUp(userId, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    @PostMapping("/incentive")
    public ResponseEntity<WalletOperationResponseDto> grantIncentive(
            @Valid @RequestBody WalletOperationRequestDto request,
            Authentication authentication) {

        Long userId = getUserIdFromAuth(authentication);
        log.info("Incentive grant request received for user {}", userId);

        WalletOperationResponseDto response = walletService.grantIncentive(userId, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    @PostMapping("/spend")
    public ResponseEntity<WalletOperationResponseDto> spendCurrency(
            @Valid @RequestBody WalletOperationRequestDto request,
            Authentication authentication) {

        Long userId = getUserIdFromAuth(authentication);
        log.info("Spend request received for user {}", userId);

        WalletOperationResponseDto response = walletService.spend(userId, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    @GetMapping("/balance")
    public ResponseEntity<BalanceResponseDto> getBalance(
            @RequestParam AssetType assetType,
            Authentication authentication) {

        Long userId = getUserIdFromAuth(authentication);
        log.info("Balance request received for user {} and asset type {}", userId, assetType);

        BalanceResponseDto response = walletService.getBalance(userId, assetType);
        return ResponseEntity.ok(response);
    }


    private Long getUserIdFromAuth(Authentication authentication) {
        String email = authentication.getName();

        if (authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }

        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Unable to extract user ID from authentication");
        }
    }
}
