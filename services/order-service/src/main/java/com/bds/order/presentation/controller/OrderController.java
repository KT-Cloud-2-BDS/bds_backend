package com.bds.order.presentation.controller;

import com.bds.common.annotation.LoginUser;
import com.bds.common.dto.CurrentUser;
import com.bds.common.events.order.OrderCreatedDirectEvent;
import com.bds.common.events.order.OrderCreatedEvent;
import com.bds.order.application.OrderService;
import com.bds.order.infrastructure.messaging.DirectEventPublisher;
import com.bds.order.infrastructure.messaging.OrderEventPublisher;
import com.bds.order.presentation.dto.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/orders")
@Validated
public class OrderController {

    private final OrderService orderService;
    private final DirectEventPublisher directEventPublisher;

    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> listOrders(
            @LoginUser CurrentUser user,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size
    ) {
        Long memberId = user.id();
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(orderService.getAllOrders(memberId, pageable));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponseDto> detailOrder(
            @LoginUser CurrentUser user,
            @PathVariable Long orderId

    ) {
        Long memberId = user.id();
        return ResponseEntity.ok(orderService.getOrderDetail(memberId, orderId));
    }

    @PostMapping("/billing")
    public ResponseEntity<BillingResponseDto> createBilling(
            @LoginUser CurrentUser user,
            @Valid @RequestBody BillingRequestDto reqDto
    ) {
        Long memberId = user.id();
        directEventPublisher.publish(OrderCreatedDirectEvent.of(memberId,12L));
        return ResponseEntity.ok(orderService.createBilling(memberId, reqDto));
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderCancelResponseDto> cancelOrder(
            @LoginUser CurrentUser user,
            @PathVariable Long orderId
    ) {
        Long memberId = user.id();
        return ResponseEntity.ok(orderService.cancelOrder(memberId, orderId));
    }

    @PostMapping
    public ResponseEntity<OrderCreateResponseDto> createOrder(
            @LoginUser CurrentUser user,
            @Valid @RequestBody OrderCreateRequestDto reqDto
    ) {
        Long memberId = user.id();
        return ResponseEntity.ok(orderService.createOrder(memberId, reqDto));
    }
}
