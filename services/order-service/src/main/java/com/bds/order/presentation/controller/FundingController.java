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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fundings")
public class FundingController {

    private final FundingService fundingService;

    @GetMapping
    public ResponseEntity<List<FundingListResponseDto>> listFundings(
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(fundingService.getFundings(status));
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
