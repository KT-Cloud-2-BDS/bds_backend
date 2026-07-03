package com.bds.order.infrastructure.order;

import com.bds.order.domain.order.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByMemberId(Long memberId, Pageable pageable);
}
