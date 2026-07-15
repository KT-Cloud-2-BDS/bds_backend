package com.bds.order.infrastructure.funding;

import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.infrastructure.reward.RewardJpaRepository;
import com.bds.order.presentation.dto.FundingCreateRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FundingPersistenceAdaptorUnitTest {

    private static final LocalDateTime NOW = LocalDateTime.now();

    @Mock
    private FundingJpaRepository fundingJpaRepository;

    @Mock
    private RewardJpaRepository rewardJpaRepository;

    @Mock
    private FundingMapper fundingMapper;

    @InjectMocks
    private FundingPersistenceAdaptor fundingPersistenceAdaptor;

    private FundingJpaEntity createEntity(Long id, FundingStatus status) {
        return new FundingJpaEntity(
                id, "Title", 100L, status,
                NOW.minusDays(10), NOW.plusDays(30), NOW.plusDays(60),
                0, 1000000L, 500000L, false, new ArrayList<>()
        );
    }

    private Funding createDomain(Long id, FundingStatus status) {
        return Funding.of(id, "Title", 100L, status,
                NOW.minusDays(10), NOW.plusDays(30), NOW.plusDays(60),
                0, 1000000L, 500000L, false, NOW, NOW);
    }

    @Nested
    @DisplayName("ID로 펀딩 조회")
    class FindByIdTest {

        @Test
        void 존재하는_펀딩을_조회하면_도메인_객체를_반환한다() {
            FundingJpaEntity entity = createEntity(1L, FundingStatus.ACTIVE);
            Funding funding = createDomain(1L, FundingStatus.ACTIVE);

            given(fundingJpaRepository.findById(1L)).willReturn(Optional.of(entity));
            given(fundingMapper.toDomain(entity)).willReturn(funding);

            Optional<Funding> result = fundingPersistenceAdaptor.findById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1L);
        }

        @Test
        void 존재하지_않는_펀딩을_조회하면_빈값을_반환한다() {
            given(fundingJpaRepository.findById(999L)).willReturn(Optional.empty());

            Optional<Funding> result = fundingPersistenceAdaptor.findById(999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("전체 펀딩 조회")
    class FindAllTest {

        @Test
        void 전체_펀딩_목록을_반환한다() {
            FundingJpaEntity entity1 = createEntity(1L, FundingStatus.ACTIVE);
            FundingJpaEntity entity2 = createEntity(2L, FundingStatus.SCHEDULED);
            Funding funding1 = createDomain(1L, FundingStatus.ACTIVE);
            Funding funding2 = createDomain(2L, FundingStatus.SCHEDULED);

            given(fundingJpaRepository.findAll()).willReturn(List.of(entity1, entity2));
            given(fundingMapper.toDomain(entity1)).willReturn(funding1);
            given(fundingMapper.toDomain(entity2)).willReturn(funding2);

            List<Funding> result = fundingPersistenceAdaptor.findAll();

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("상태별 펀딩 조회")
    class FindByStatusTest {

        @Test
        void 해당_상태의_펀딩_목록을_반환한다() {
            FundingJpaEntity entity = createEntity(1L, FundingStatus.ACTIVE);
            Funding funding = createDomain(1L, FundingStatus.ACTIVE);

            given(fundingJpaRepository.findByStatus(FundingStatus.ACTIVE)).willReturn(List.of(entity));
            given(fundingMapper.toDomain(entity)).willReturn(funding);

            List<Funding> result = fundingPersistenceAdaptor.findByStatus(FundingStatus.ACTIVE);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(FundingStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("판정 대기 펀딩 조회")
    class FindFundingsReadyForJudgmentTest {

        @Test
        void holdTo가_지난_ACTIVE_HOLDING_펀딩을_반환한다() {
            FundingJpaEntity entity = createEntity(1L, FundingStatus.ACTIVE);
            Funding funding = createDomain(1L, FundingStatus.ACTIVE);

            given(fundingJpaRepository.findFundingsReadyForJudgment(NOW)).willReturn(List.of(entity));
            given(fundingMapper.toDomain(entity)).willReturn(funding);

            List<Funding> result = fundingPersistenceAdaptor.findFundingsReadyForJudgment(NOW);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("상태 + startAt 이전/동일 펀딩 조회")
    class FindByStatusAndStartAtBeforeOrEqualTest {

        @Test
        void 해당_조건의_펀딩을_반환한다() {
            FundingJpaEntity entity = createEntity(1L, FundingStatus.SCHEDULED);
            Funding funding = createDomain(1L, FundingStatus.SCHEDULED);

            given(fundingJpaRepository.findByStatusAndStartAtBeforeOrEqual(FundingStatus.SCHEDULED, NOW))
                    .willReturn(List.of(entity));
            given(fundingMapper.toDomain(entity)).willReturn(funding);

            List<Funding> result = fundingPersistenceAdaptor.findByStatusAndStartAtBeforeOrEqual(FundingStatus.SCHEDULED, NOW);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("상태 + startAt 이후 펀딩 조회")
    class FindByStatusAndStartAtAfterTest {

        @Test
        void 해당_조건의_펀딩을_반환한다() {
            FundingJpaEntity entity = createEntity(1L, FundingStatus.SCHEDULED);
            Funding funding = createDomain(1L, FundingStatus.SCHEDULED);

            given(fundingJpaRepository.findByStatusAndStartAtAfter(FundingStatus.SCHEDULED, NOW))
                    .willReturn(List.of(entity));
            given(fundingMapper.toDomain(entity)).willReturn(funding);

            List<Funding> result = fundingPersistenceAdaptor.findByStatusAndStartAtAfter(FundingStatus.SCHEDULED, NOW);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("상태 + holdTo 이후 펀딩 조회")
    class FindByStatusAndHoldToAfterTest {

        @Test
        void 해당_조건의_펀딩을_반환한다() {
            FundingJpaEntity entity = createEntity(1L, FundingStatus.ACTIVE);
            Funding funding = createDomain(1L, FundingStatus.ACTIVE);

            given(fundingJpaRepository.findByStatusAndHoldToAfter(FundingStatus.ACTIVE, NOW))
                    .willReturn(List.of(entity));
            given(fundingMapper.toDomain(entity)).willReturn(funding);

            List<Funding> result = fundingPersistenceAdaptor.findByStatusAndHoldToAfter(FundingStatus.ACTIVE, NOW);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("비관적 락으로 펀딩 조회")
    class FindByIdForUpdateTest {

        @Test
        void 락을_잡고_펀딩을_반환한다() {
            FundingJpaEntity entity = createEntity(1L, FundingStatus.ACTIVE);
            Funding funding = createDomain(1L, FundingStatus.ACTIVE);

            given(fundingJpaRepository.findByIdForUpdate(1L)).willReturn(Optional.of(entity));
            given(fundingMapper.toDomain(entity)).willReturn(funding);

            Optional<Funding> result = fundingPersistenceAdaptor.findByIdForUpdate(1L);

            assertThat(result).isPresent();
        }

        @Test
        void 존재하지_않으면_빈값을_반환한다() {
            given(fundingJpaRepository.findByIdForUpdate(999L)).willReturn(Optional.empty());

            Optional<Funding> result = fundingPersistenceAdaptor.findByIdForUpdate(999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("펀딩 저장")
    class SaveTest {

        @Test
        void 펀딩을_저장한다() {
            Funding funding = createDomain(1L, FundingStatus.SUCCESS);
            FundingJpaEntity entity = createEntity(1L, FundingStatus.SUCCESS);

            given(fundingMapper.toJpaEntity(funding)).willReturn(entity);

            fundingPersistenceAdaptor.save(funding);

            verify(fundingJpaRepository).save(entity);
        }
    }

    @Nested
    @DisplayName("펀딩 + 리워드 동시 저장")
    class SaveWithRewardsTest {

        @Test
        void 펀딩과_리워드를_함께_저장하고_도메인을_반환한다() {
            Funding funding = createDomain(null, FundingStatus.SCHEDULED);
            FundingJpaEntity entity = createEntity(null, FundingStatus.SCHEDULED);
            FundingJpaEntity savedEntity = createEntity(1L, FundingStatus.SCHEDULED);
            Funding savedFunding = createDomain(1L, FundingStatus.SCHEDULED);

            List<FundingCreateRequestDto.RewardCreateDto> rewards = List.of(
                    new FundingCreateRequestDto.RewardCreateDto(
                            "리워드A", "설명A", 100, null, 10000L, NOW.plusDays(60), 3000L)
            );

            given(fundingMapper.toJpaEntity(funding)).willReturn(entity);
            given(fundingJpaRepository.save(entity)).willReturn(savedEntity);
            given(fundingMapper.toDomain(savedEntity)).willReturn(savedFunding);

            Funding result = fundingPersistenceAdaptor.saveWithRewards(funding, rewards);

            assertThat(result.getId()).isEqualTo(1L);
            verify(fundingJpaRepository).save(entity);
        }

        @Test
        void badgeType이_있는_리워드도_함께_저장한다() {
            Funding funding = createDomain(null, FundingStatus.SCHEDULED);
            FundingJpaEntity entity = createEntity(null, FundingStatus.SCHEDULED);
            FundingJpaEntity savedEntity = createEntity(1L, FundingStatus.SCHEDULED);
            Funding savedFunding = createDomain(1L, FundingStatus.SCHEDULED);

            List<FundingCreateRequestDto.RewardCreateDto> rewards = List.of(
                    new FundingCreateRequestDto.RewardCreateDto(
                            "리워드A", "설명A", 100, "EARLY_BIRD", 10000L, NOW.plusDays(60), 3000L)
            );

            given(fundingMapper.toJpaEntity(funding)).willReturn(entity);
            given(fundingJpaRepository.save(entity)).willReturn(savedEntity);
            given(fundingMapper.toDomain(savedEntity)).willReturn(savedFunding);

            Funding result = fundingPersistenceAdaptor.saveWithRewards(funding, rewards);

            assertThat(result.getId()).isEqualTo(1L);
        }
    }
}
