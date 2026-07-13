package com.bds.order.infrastructure.reward;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RewardJpaRepository extends JpaRepository<RewardJpaEntity, Long> {
    @Query("""
              SELECT r FROM RewardJpaEntity r
              WHERE r.id IN :ids AND r.funding.id = :fundingId
            """)
    List<RewardJpaEntity> findAllByIdAndFundingId(@Param("ids") List<Long> ids, @Param("fundingId") Long fundingId);

    @Modifying
    @Query("UPDATE RewardJpaEntity r SET r.remainQty = r.remainQty + :qty WHERE r.id = :rewardId")
    void increaseRemainQty(@Param("rewardId") Long rewardId, @Param("qty") int qty);

    @Modifying
    @Query("""
              UPDATE RewardJpaEntity r SET r.remainQty = r.remainQty - :qty
              WHERE r.id = :id AND r.remainQty >= :qty
            """)
    int decreaseStock(@Param("id") Long id, @Param("qty") int qty);
}