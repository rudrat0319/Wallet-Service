package com.walletService.Repository;

import com.walletService.Entity.AssetType;
import com.walletService.Entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId AND w.assetType = :assetType")
    Optional<Wallet> findByUserIdAndAssetTypeForUpdate(@Param("userId") Long userId,
                                                       @Param("assetType") AssetType assetType);


    Optional<Wallet> findByUserIdAndAssetType(Long userId, AssetType assetType);


    List<Wallet> findByUserId(Long userId);


    boolean existsByUserIdAndAssetType(Long userId, AssetType assetType);
}