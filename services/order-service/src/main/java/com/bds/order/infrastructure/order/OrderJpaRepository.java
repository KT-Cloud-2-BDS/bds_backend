package com.bds.order.infrastructure.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {

    @Query("SELECT new com.bds.order.infrastructure.order.OrderListProjection(" +
            "o.id, o.orderNo, o.status, o.amount, o.createdAt, " +
            "f.title, f.creatorId, f.holdTo, f.isSuccess) " +
            "FROM OrderJpaEntity o " +
            "JOIN o.orderRewards orw " +
            "JOIN orw.reward r " +
            "JOIN r.funding f " +
            "WHERE o.memberId = :memberId")
    Page<OrderListProjection> findOrderListWithFunding(@Param("memberId") Long memberId, Pageable pageable);
}
