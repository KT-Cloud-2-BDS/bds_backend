package com.bds.order.application;

import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.funding.FundingRepository;
import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.domain.funding.FundingType;
import com.bds.order.domain.reward.Reward;
import com.bds.order.domain.reward.RewardRepository;
import com.bds.order.global.exception.BusinessException;
import com.bds.order.global.exception.ErrorCode;
import com.bds.order.infrastructure.scheduler.FundingTaskScheduler;
import com.bds.order.presentation.dto.FundingCreateRequestDto;
import com.bds.order.presentation.dto.FundingCreateResponseDto;
import com.bds.order.presentation.dto.FundingDetailResponseDto;
import com.bds.order.presentation.dto.FundingListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FundingService {

    private final FundingRepository fundingRepository;
    private final RewardRepository rewardRepository;
    private final FundingTaskScheduler fundingTaskScheduler;

    public Page<FundingListResponseDto> getFundings(String type, String status, int page, int size) {
        FundingType fundingType = switch (type) {
            case "RESERVED" -> FundingType.RESERVED;
            default -> FundingType.INSTANT;
        };

        Pageable pageable;
        List<FundingStatus> statuses;

        switch (status) {
            case "SCHEDULED" -> {
                statuses = List.of(FundingStatus.SCHEDULED);
                pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "startAt"));
            }
            case "ACTIVE" -> {
                statuses = List.of(FundingStatus.ACTIVE);
                pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "holdTo"));
            }
            case "CLOSED" -> {
                statuses = List.of(FundingStatus.SUCCESS, FundingStatus.FAILED);
                pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "holdTo"));
            }
            default -> {
                statuses = List.of(FundingStatus.SCHEDULED, FundingStatus.ACTIVE, FundingStatus.SUCCESS, FundingStatus.FAILED);
                pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "startAt"));
            }
        }

        Page<Funding> fundingPage = fundingRepository.findByTypeAndStatusIn(fundingType, statuses, pageable);
        return fundingPage.map(FundingListResponseDto::from);
    }

    public FundingDetailResponseDto getFundingDetail(Long fundingId) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FUNDING_NOT_FOUND));

        List<Reward> rewards = rewardRepository.findByFundingId(fundingId);

        return FundingDetailResponseDto.from(funding, rewards);
    }

    @Transactional
    public FundingCreateResponseDto createFunding(Long creatorId, boolean isMaker, FundingCreateRequestDto request) {
        if (!isMaker) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        LocalDateTime now = LocalDateTime.now();

        if (request.startAt().isBefore(now)) {
            throw new BusinessException(ErrorCode.INVALID_START_DATE);
        }

        if (!request.startAt().isBefore(request.holdTo())) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }

        if (request.payAt().isBefore(request.holdTo())) {
            throw new BusinessException(ErrorCode.INVALID_PAY_DATE);
        }

        Funding funding = Funding.create(
                request.title(), creatorId, request.goalAmount(),
                request.startAt(), request.holdTo(), request.payAt(),
                request.type()
        );

        Funding savedFunding = fundingRepository.saveWithRewards(funding, request.rewards());

        fundingTaskScheduler.scheduleHoldToJudgment(savedFunding.getId(), savedFunding.getHoldTo());

        return FundingCreateResponseDto.from(savedFunding);
    }
}
