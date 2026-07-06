package com.bds.order.presentation.controller;

import com.bds.order.application.OrderService;
import com.bds.order.presentation.dto.OrderResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/orders/")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> listOrders(
            @RequestHeader("X-Member-Id") Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(orderService.getAllOrders(memberId, pageable));
    }
}
