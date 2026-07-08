package com.bds.order.presentation.controller;

import com.bds.common.annotation.LoginUser;
import com.bds.common.dto.CurrentUser;
import com.bds.order.application.OrderService;
import com.bds.order.presentation.dto.BillingRequestDto;
import com.bds.order.presentation.dto.BillingResponseDto;
import com.bds.order.presentation.dto.OrderDetailResponseDto;
import com.bds.order.presentation.dto.OrderResponseDto;
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
@RequestMapping("api/orders/")
@Validated
public class OrderController {

    private final OrderService orderService;

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
    public ResponseEntity<OrderDetailResponseDto> listOrders(
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
        return ResponseEntity.ok(orderService.createBilling(memberId, reqDto));
    }
}
