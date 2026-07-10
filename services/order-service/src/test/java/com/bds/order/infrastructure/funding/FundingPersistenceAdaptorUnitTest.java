package com.bds.order.infrastructure.funding;

import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.funding.FundingStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FundingPersistenceAdaptorUnitTest {

    @Mock
    private FundingJpaRepository fundingJpaRepository;

    @Mock
    private FundingMapper fundingMapper;

    @InjectMocks
    private FundingPersistenceAdaptor fundingPersistenceAdaptor;

    @Nested
    @DisplayName("ID로 펀딩 조회")
    class FindByIdTest {

        @Test
        void 존재하는_펀딩을_조회하면_도메인_객체를_반환한다() {
            LocalDateTime now = LocalDateTime.now();
            FundingJpaEntity entity = new FundingJpaEntity(
                    1L, "Title", 100L, FundingStatus.ACTIVE,
                    now.minusDays(10), now.plusDays(30), now.plusDays(60),
                    5, 1000000L, 500000L, false, new ArrayList<>()
            );
            Funding funding = Funding.of(1L, "Title", 100L, FundingStatus.ACTIVE,
                    now.minusDays(10), now.plusDays(30), now.plusDays(60),
                    5, 1000000L, 500000L, false, now, now);

            given(fundingJpaRepository.findById(1L)).willReturn(Optional.of(entity));
            given(fundingMapper.toDomain(entity)).willReturn(funding);

            Optional<Funding> result = fundingPersistenceAdaptor.findById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1L);
            assertThat(result.get().getTitle()).isEqualTo("Title");
        }

        @Test
        void 존재하지_않는_펀딩을_조회하면_빈값을_반환한다() {
            given(fundingJpaRepository.findById(999L)).willReturn(Optional.empty());

            Optional<Funding> result = fundingPersistenceAdaptor.findById(999L);

            assertThat(result).isEmpty();
        }
    }
}