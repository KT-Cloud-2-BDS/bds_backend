package com.bds.order.infrastructure.order;

import com.bds.order.domain.order.CancelReason;
import com.bds.order.domain.order.OrderStatus;
import com.bds.order.infrastructure.orderReward.OrderRewardJpaEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "`order`")
@EntityListeners(AuditingEntityListener.class)
public class OrderJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String orderNo;

    private Long memberId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Long totalRewardAmount;

    private Long totalShippingCharge;

    private CancelReason cancelReason;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime cancelledAt;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<OrderRewardJpaEntity> orderRewards = new ArrayList<>();

    @PrePersist
    public void generateOrderNumber() {
        this.orderNo = "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}