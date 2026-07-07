package com.bds.order.infrastructure.reward;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RewardJpaRepository extends JpaRepository<RewardJpaEntity, Long> {
    @Query("""
              SELECT r FROM RewardJpaEntity r
              WHERE r.id IN :ids AND r.funding.id = :fundingId
            """)
    List<RewardJpaEntity> findAllByIdAndFundingId(@Param("ids") List<Long> ids, @Param("fundingId") Long fundingId);
}