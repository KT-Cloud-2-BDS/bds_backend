package com.bds.order.infrastructure.reward;

import com.bds.order.domain.reward.BadgeType;
import com.bds.order.infrastructure.funding.FundingJpaEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reward")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RewardJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funding_id", nullable = false)
    private FundingJpaEntity funding;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 200)
    private String description;

    @Column(nullable = false)
    private int limitQty;

    private Integer remainQty;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BadgeType badgeType;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private LocalDateTime offerAt;

    @Column(nullable = false)
    private int shippingCharge;
}