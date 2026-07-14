package com.bds.order.infrastructure.funding;

import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.infrastructure.common.BaseEntity;
import com.bds.order.infrastructure.reward.RewardJpaEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "funding")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FundingJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private Long creatorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FundingStatus status;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime holdTo;

    @Column(nullable = false)
    private LocalDateTime payAt;

    @Column(nullable = false)
    private int participationCnt;

    @Column(nullable = false)
    private Long goalAmount;

    @Column(nullable = false)
    private Long currentAmount;

    private Boolean isSuccess;

    @OneToMany(mappedBy = "funding", fetch = FetchType.LAZY)
    private List<RewardJpaEntity> rewards = new ArrayList<>();
}