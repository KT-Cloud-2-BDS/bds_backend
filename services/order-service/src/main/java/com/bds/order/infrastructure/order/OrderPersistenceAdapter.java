package com.bds.order.infrastructure.order;

import com.bds.order.domain.order.Order;
import com.bds.order.domain.order.OrderRepository;
import com.bds.order.domain.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderPersistenceAdapter implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;
    private final OrderMapper orderMapper;

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = orderMapper.toJpaEntity(order);
        OrderJpaEntity saved = orderJpaRepository.save(entity);
        return orderMapper.toDomain(saved);
    }

    @Override
    public void deleteAll() {
        orderJpaRepository.deleteAll();
    }

    @Override
    public List<OrderListProjection> findOrderListWithFunding(Long memberId, Pageable pageable) {
        return orderJpaRepository.findOrderListWithFunding(memberId, pageable).getContent();
    }

    @Override
    public Optional<OrderDetailProjection> findOrderDetailWithFunding(Long memberId, Long orderId) {
        return orderJpaRepository.findOrderWithFunding(memberId, orderId);
    }

    @Override
    public Optional<Order> findById(Long orderId) {
        return orderJpaRepository.findByIdWithRewards(orderId).map(orderMapper::toDomain);
    }

    @Override
    public List<Long> findOrderIdsByFundingIdAndStatus(Long fundingId, OrderStatus status, Long lastOrderId, int size) {
        return orderJpaRepository.findOrderIdsByFundingIdAndStatus(fundingId, status, lastOrderId, PageRequest.of(0, size));
    }

    @Override
    public Optional<Order> findByIdForUpdate(Long orderId) {
        return orderJpaRepository.findByIdForUpdate(orderId).map(orderMapper::toDomain);
    }

    @Override
    public Optional<String> findFundingTitleByOrderId(Long orderId) {
        return orderJpaRepository.findFundingTitleByOrderId(orderId);
    }
}