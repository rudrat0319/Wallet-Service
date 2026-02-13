package com.walletService.Dto;

import com.walletService.Entity.AssetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceResponseDto {

    private Long userId;

    private AssetType assetType;

    private BigDecimal balance;
}
