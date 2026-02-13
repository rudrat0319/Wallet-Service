package com.walletService.Dto;

import com.walletService.Entity.AssetType;
import com.walletService.Entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionHistoryResponseDto {

    private AssetType assetType;

    private BigDecimal currentBalance;

    private List<TransactionDto> transactions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TransactionDto {

        private Long id;

        private TransactionType type;

        private BigDecimal amount;

        private BigDecimal balanceAfter;

        private String description;

        private String referenceId;

        private LocalDateTime timestamp;
    }
}
