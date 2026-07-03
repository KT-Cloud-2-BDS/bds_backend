package com.bds.order.infrastructure.order;

import com.bds.order.domain.order.Order;
import com.bds.order.domain.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryAdaptor implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;

    public List<Order> findAllByMemberId(Long id, Pageable pageable) {
        return orderJpaRepository.findAllByMemberId(id, pageable);
    }

}
