package com.walletService.Dto;

import com.walletService.Entity.AssetType;
import com.walletService.Entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletOperationResponseDto {

    private Long transactionId;

    private TransactionType transactionType;

    private BigDecimal amount;

    private BigDecimal balanceAfter;

    private AssetType assetType;

    private String description;

    private String referenceId;

    private LocalDateTime timestamp;

    private String message;
}
