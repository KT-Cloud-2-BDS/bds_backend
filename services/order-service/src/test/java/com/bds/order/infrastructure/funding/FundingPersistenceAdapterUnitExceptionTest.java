package com.bds.order.infrastructure.funding;

import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.fixture.FundingFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FundingPersistenceAdapterUnitExceptionTest {

    @InjectMocks
    private FundingPersistenceAdapter fundingPersistenceAdapter;

    @Mock
    private FundingJpaRepository fundingJpaRepository;

    @Mock
    private FundingMapper fundingMapper;

    @Test
    void save_기존_펀딩이_DB에_없으면_예외를_던진다() {
        Funding funding = FundingFixture.createFunding(999L, FundingStatus.ACTIVE, 1000000L, 500000L,
                LocalDateTime.now().minusDays(30), LocalDateTime.now().minusDays(1));

        when(fundingJpaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fundingPersistenceAdapter.save(funding))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("[FundingPersistenceAdapter] Funding not found: fundingId=999");
    }
}