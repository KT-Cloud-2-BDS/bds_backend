package com.bds.order.domain.order;

import com.bds.order.domain.common.BaseEntity;
import com.bds.order.presentation.dto.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "`order`")
public class Order extends BaseEntity {
    @Id
    private Long id;

    @Column(unique = true)
    private String orderNo;

    private Long memberId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Long amount;

    private String cancelReason;

    @PrePersist
    public void generateOrderNumber() {
        this.orderNo = "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
