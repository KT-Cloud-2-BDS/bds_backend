package com.bds.order.infrastructure.order;

import com.bds.order.domain.order.Order;
import com.bds.order.domain.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

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
    public List<OrderListProjection> findOrderListByMemberId(Long memberId, Pageable pageable) {
        return orderJpaRepository.findOrderListWithFunding(memberId, pageable).getContent();
    }
}