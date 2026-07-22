package com.bds.order.application;

import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.funding.FundingRepository;
import com.bds.order.domain.funding.FundingStatus;
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

    public List<FundingListResponseDto> getFundings(String status) {
        List<Funding> fundings;
        if (status != null && !status.isBlank()) {
            try {
                fundings = fundingRepository.findByStatus(FundingStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
        } else {
            fundings = fundingRepository.findAll();
        }

        return fundings.stream().map(FundingListResponseDto::from).toList();
    }

    public FundingDetailResponseDto getFundingDetail(Long fundingId) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FUNDING_NOT_FOUND));

        List<Reward> rewards = rewardRepository.findByFundingId(fundingId);

        return FundingDetailResponseDto.from(funding, rewards);
    }

    @Transactional
    public FundingCreateResponseDto createFunding(Long creatorId, String role, FundingCreateRequestDto request) {
        if (!"MAKER".equalsIgnoreCase(role)) {
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
