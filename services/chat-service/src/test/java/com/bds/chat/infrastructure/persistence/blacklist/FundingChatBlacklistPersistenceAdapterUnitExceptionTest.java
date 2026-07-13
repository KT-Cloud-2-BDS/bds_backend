package com.bds.chat.infrastructure.persistence.blacklist;

import com.bds.chat.domain.blackList.BlacklistStatus;
import com.bds.chat.domain.blackList.FundingChatBlacklist;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FundingChatBlacklistPersistenceAdapterUnitExceptionTest {

    @Mock FundingChatBlacklistJpaRepository jpaRepository;
    @Mock FundingChatBlacklistMapper mapper;
    @Mock EntityManager entityManager;

    @InjectMocks FundingChatBlacklistPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adapter, "entityManager", entityManager);
    }

    @Nested
    @DisplayName("isBlacklisted 실패 케이스")
    class IsBlacklistedFailTest {

        @Test
        void 블랙리스트에_없는_멤버는_false를_반환한다() {
            given(jpaRepository.existsByRoom_IdAndMemberIdAndStatus(10L, 99L, BlacklistStatus.ACTIVE)).willReturn(false);

            boolean result = adapter.isBlacklisted(10L, 99L);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("findBlacklist 실패 케이스")
    class FindBlacklistFailTest {

        @Test
        void 활성_블랙리스트가_없으면_빈_Optional을_반환한다() {
            given(jpaRepository.findByRoom_IdAndMemberIdAndStatus(10L, 99L, BlacklistStatus.ACTIVE))
                    .willReturn(Optional.empty());

            Optional<FundingChatBlacklist> result = adapter.findBlacklist(10L, 99L);

            assertThat(result).isEmpty();
        }
    }
}
