package com.bds.order.infrastructure.orderReward;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRewardJpaRepository extends JpaRepository<OrderRewardJpaEntity, Long> {

    @Query("SELECT new com.bds.order.infrastructure.orderReward.OrderRewardDetailProjection(" +
            "orw.id, orw.qty, orw.amount, orw.shippingCharge, " +
            "r.name, r.badgeType) " +
            "FROM OrderRewardJpaEntity orw " +
            "JOIN orw.reward r " +
            "WHERE orw.order.id = :orderId")
    List<OrderRewardDetailProjection> findOrderRewardDetailsWithReward(@Param("orderId") Long orderId);
}
