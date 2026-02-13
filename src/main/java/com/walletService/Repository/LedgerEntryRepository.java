package com.walletService.Repository;

import com.walletService.Entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {


    List<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(Long walletId);


    @Query("SELECT l FROM LedgerEntry l WHERE l.wallet.id = :walletId " +
            "AND l.createdAt BETWEEN :fromTime AND :toTime " +
            "ORDER BY l.createdAt DESC")
    List<LedgerEntry> findByWalletIdAndCreatedAtBetween(@Param("walletId") Long walletId,
                                                        @Param("fromTime") LocalDateTime fromTime,
                                                        @Param("toTime") LocalDateTime toTime);


    Optional<LedgerEntry> findByIdempotencyKey(String idempotencyKey);


    @Query("SELECT l FROM LedgerEntry l WHERE l.wallet.id = :walletId " +
            "ORDER BY l.createdAt DESC")
    List<LedgerEntry> findTopNByWalletId(@Param("walletId") Long walletId);
}