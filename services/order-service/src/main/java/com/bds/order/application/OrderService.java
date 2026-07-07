package com.bds.order.application;

import com.bds.order.domain.order.Order;
import com.bds.order.domain.order.OrderRepository;
import com.bds.order.presentation.dto.OrderResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    public List<OrderResponseDto> getAllOrders(Long memberId, Pageable pageable) {
        List<Order> orderList = orderRepository.findAllByMemberId(memberId, pageable);

        return orderList.stream()
                .map(order -> OrderResponseDto.from(order)).toList();
    }
}
