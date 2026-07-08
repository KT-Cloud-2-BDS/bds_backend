package com.bds.order.domain.order;

import com.bds.order.infrastructure.order.OrderDetailProjection;
import com.bds.order.infrastructure.order.OrderListProjection;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);

    void deleteAll();

    List<OrderListProjection> findOrderListByMemberId(Long memberId, Pageable pageable);

    Optional<OrderDetailProjection> findOrderByMemberId(Long memberId, Long orderId);
}
