package com.bds.order.infrastructure.orderReward;

import com.bds.order.infrastructure.order.OrderJpaEntity;
import com.bds.order.infrastructure.reward.RewardJpaEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_reward")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class OrderRewardJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderJpaEntity order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_id", nullable = false)
    private RewardJpaEntity reward;

    @Column(nullable = false)
    private int qty;

    private Long shippingCharge;

    private Long amount;

    public Long getOrderId() {
        return order.getId();
    }

    public Long getRewardId() {
        return reward.getId();
    }
}