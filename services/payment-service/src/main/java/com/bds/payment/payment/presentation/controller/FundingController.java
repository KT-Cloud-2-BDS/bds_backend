package com.bds.payment.payment.presentation.controller;

import com.bds.payment.payment.application.funding.FundingService;
import com.bds.payment.payment.presentation.request.FundingPaymentRequestDto;
import com.bds.payment.payment.presentation.response.FundingPaymentResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
public class FundingController {

    private final FundingService fundingService;

    //TODO: Kafka를 사용함에 따라 컨트롤러 미사용으로 인지

    @PostMapping("/funding")
    public FundingPaymentResponseDto funding(FundingPaymentRequestDto dto) {
        return fundingService.funding(dto);
    }

//    @PostMapping("/refund")
//    public RefundResponseDto refund(@RequestHeader("X-Member-Id")Long memberId, Long orderId) {
//
//    } //TODO: 주문과의 연계 고려중
}
