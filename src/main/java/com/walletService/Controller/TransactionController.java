package com.walletService.Controller;

import com.walletService.Dto.TransactionHistoryResponseDto;
import com.walletService.Entity.AssetType;
import com.walletService.Service.TransactionHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;


@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionHistoryService transactionHistoryService;

    @GetMapping("/transactions")
    public ResponseEntity<TransactionHistoryResponseDto> getTransactionHistory(
            @RequestParam AssetType assetType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toTime,
            @RequestParam(required = false, defaultValue = "100") int limit,
            Authentication authentication) {

        Long userId = getUserIdFromAuth(authentication);
        log.info("Transaction history request for user {} and asset type {}", userId, assetType);

        TransactionHistoryResponseDto response;

        if (fromTime != null && toTime != null) {
            response = transactionHistoryService.getTransactionHistory(userId, assetType, fromTime, toTime);
        } else {
            response = transactionHistoryService.getRecentTransactions(userId, assetType, limit);
        }

        return ResponseEntity.ok(response);
    }


    private Long getUserIdFromAuth(Authentication authentication) {
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