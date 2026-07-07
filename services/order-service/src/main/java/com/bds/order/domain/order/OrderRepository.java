package com.bds.order.domain.order;

import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderRepository {
    Order save(Order order);

    void deleteAll();

    List<Order> findAllByMemberId(Long id, Pageable pageable);
}
