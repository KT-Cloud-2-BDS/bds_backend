package com.bds.order.domain.order;

import com.bds.order.domain.common.BaseEntity;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String orderNo;

    private Long memberId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Long amount;

    private String cancelReason;

    public static Order create(Long memberId, Long amount, OrderStatus status) {
        Order order = new Order();
        order.memberId = memberId;
        order.amount = amount;
        order.status = (status != null) ? status : OrderStatus.PENDING;
        return order;
    }

    @PrePersist
    public void generateOrderNumber() {
        this.orderNo = "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
