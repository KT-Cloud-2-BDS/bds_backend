package com.bds.order.domain.funding;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Funding {

    private Long id;
    private String title;
    private Long creatorId;
    private FundingStatus status;
    private FundingType type;
    private LocalDateTime startAt;
    private LocalDateTime holdTo;
    private LocalDateTime payAt;
    private int participationCnt;
    private Long goalAmount;
    private Long currentAmount;
    private Boolean isSuccess;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Funding of(Long id, String title, Long creatorId, FundingStatus status, FundingType type,
                             LocalDateTime startAt, LocalDateTime holdTo, LocalDateTime payAt,
                             int participationCnt, Long goalAmount, Long currentAmount,
                             Boolean isSuccess, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Funding(id, title, creatorId, status, type, startAt, holdTo, payAt,
                participationCnt, goalAmount, currentAmount, isSuccess, createdAt, updatedAt);
    }

    public static Funding create(String title, Long creatorId, Long goalAmount,
                                 LocalDateTime startAt, LocalDateTime holdTo, LocalDateTime payAt,
                                 FundingType type) {
        return new Funding(
                null, title, creatorId, FundingStatus.SCHEDULED,
                type == null ? FundingType.INSTANT : type, startAt, holdTo, payAt,
                0, goalAmount, 0L, false,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    public boolean isFundingPeriod(LocalDateTime now) {
        return !now.isBefore(startAt) && now.isBefore(holdTo);
    }

    public void markSuccess() {
        this.isSuccess = true;
        this.status = FundingStatus.SUCCESS;
    }

    public void markFailed() {
        this.isSuccess = false;
        this.status = FundingStatus.FAILED;
    }

    public void activate() {
        this.status = FundingStatus.ACTIVE;
    }
}