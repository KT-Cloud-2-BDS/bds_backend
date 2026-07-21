package com.bds.order.domain.order;

import com.bds.order.infrastructure.order.OrderDetailProjection;
import com.bds.order.infrastructure.order.OrderListProjection;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);

    void deleteAll();

    Optional<Order> findById(Long orderId);

    Optional<Order> findByIdForUpdate(Long orderId);

    List<OrderListProjection> findOrderListWithFunding(Long memberId, Pageable pageable);

    Optional<OrderDetailProjection> findOrderDetailWithFunding(Long memberId, Long orderId);

    List<Long> findOrderIdsByFundingIdAndStatus(Long fundingId, OrderStatus status, Long lastOrderId, int size);

    Optional<String> findFundingTitleByOrderId(Long orderId);
}
