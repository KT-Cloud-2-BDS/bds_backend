package com.bds.order.infrastructure.order;

import com.bds.order.domain.order.OrderStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {

    @Query("SELECT DISTINCT new com.bds.order.infrastructure.order.OrderListProjection(" +
            "o.id, o.orderNo, o.status, o.totalRewardAmount, o.totalShippingCharge, o.createdAt, " +
            "f.title, f.creatorId, f.holdTo, f.isSuccess," +
            "o.updatedAt) " +
            "FROM OrderJpaEntity o " +
            "JOIN o.orderRewards orw " +
            "JOIN orw.reward r " +
            "JOIN r.funding f " +
            "WHERE o.memberId = :memberId")
    Page<OrderListProjection> findOrderListWithFunding(@Param("memberId") Long memberId, Pageable pageable);

    @Query("SELECT DISTINCT new com.bds.order.infrastructure.order.OrderDetailProjection(" +
            "o.id, o.orderNo, o.status, o.totalRewardAmount, o.totalShippingCharge, o.createdAt, " +
            "f.id, f.title, f.creatorId, f.holdTo, f.isSuccess," +
            "o.cancelledAt, o.cancelReason, o.updatedAt) " +
            "FROM OrderJpaEntity o " +
            "JOIN o.orderRewards orw " +
            "JOIN orw.reward r " +
            "JOIN r.funding f " +
            "WHERE o.memberId = :memberId AND o.id = :orderId")
    Optional<OrderDetailProjection> findOrderWithFunding(@Param("memberId") Long memberId, @Param("orderId") Long orderId);


    @Query("SELECT DISTINCT o.id FROM OrderJpaEntity o " +
            "JOIN o.orderRewards orw " +
            "JOIN orw.reward r " +
            "JOIN r.funding f " +
            "WHERE f.id = :fundingId AND o.status = :status AND o.id > :lastOrderId " +
            "ORDER BY o.id ASC")
    List<Long> findOrderIdsByFundingIdAndStatus(@Param("fundingId") Long fundingId, @Param("status") OrderStatus status, @Param("lastOrderId") Long lastOrderId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT o FROM OrderJpaEntity o JOIN FETCH o.orderRewards WHERE o.id = :orderId")
    Optional<OrderJpaEntity> findByIdForUpdate(@Param("orderId") Long orderId);

    @Query("SELECT DISTINCT f.title " +
            "FROM OrderJpaEntity o " +
            "JOIN o.orderRewards orw " +
            "JOIN orw.reward r " +
            "JOIN r.funding f " +
            "WHERE o.id = :orderId")
    Optional<String> findFundingTitleByOrderId(@Param("orderId") Long orderId);
}
