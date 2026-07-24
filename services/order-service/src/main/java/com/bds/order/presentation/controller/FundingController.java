package com.bds.order.presentation.controller;

import com.bds.common.annotation.LoginUser;
import com.bds.common.dto.CurrentUser;
import com.bds.order.application.FundingService;
import com.bds.order.presentation.dto.FundingCreateRequestDto;
import com.bds.order.presentation.dto.FundingCreateResponseDto;
import com.bds.order.presentation.dto.FundingDetailResponseDto;
import com.bds.order.presentation.dto.FundingListResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fundings")
public class FundingController {

    private final FundingService fundingService;

    @GetMapping
    public ResponseEntity<Page<FundingListResponseDto>> listFundings(
            @RequestParam(defaultValue = "INSTANT") String type,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size
    ) {
        return ResponseEntity.ok(fundingService.getFundings(type, status, page, size));
    }

    @GetMapping("/{fundingId}")
    public ResponseEntity<FundingDetailResponseDto> getFundingDetail(@PathVariable Long fundingId) {
        return ResponseEntity.ok(fundingService.getFundingDetail(fundingId));
    }

    @PostMapping
    public ResponseEntity<FundingCreateResponseDto> createFunding(
            @LoginUser CurrentUser user,
            @Valid @RequestBody FundingCreateRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fundingService.createFunding(user.id(), user.roles().contains("MAKER"), request));
    }
}
