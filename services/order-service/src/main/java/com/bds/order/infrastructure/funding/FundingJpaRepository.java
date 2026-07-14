package com.bds.order.infrastructure.funding;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FundingJpaRepository extends JpaRepository<FundingJpaEntity, Long> {

    @Query("SELECT f FROM FundingJpaEntity f " +
            "WHERE f.status = :status " +
            "AND f.startAt > :now")
    List<FundingJpaEntity> findByStatusAndStartAtAfter(@Param("status") String status, @Param("now") LocalDateTime now);

    @Query("SELECT f FROM FundingJpaEntity f " +
            "WHERE f.status = :status " +
            "AND f.holdTo > :now")
    List<FundingJpaEntity> findByStatusAndHoldToAfter(@Param("status") String status, @Param("now") LocalDateTime now);

    @Query("SELECT f FROM FundingJpaEntity f " +
            "WHERE f.status NOT IN ('SUCCESS', 'FAILED') " +
            "AND f.holdTo <= :now")
    List<FundingJpaEntity> findFundingsReadyForJudgment(@Param("now") LocalDateTime now);

    @Query("SELECT f FROM FundingJpaEntity f " +
            "WHERE f.status = :status " +
            "AND f.startAt <= :now")
    List<FundingJpaEntity> findByStatusAndStartAtBeforeOrEqual(@Param("status") String status, @Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT f FROM FundingJpaEntity f WHERE f.id = :fundingId")
    Optional<FundingJpaEntity> findByIdForUpdate(@Param("fundingId") Long fundingId);
}