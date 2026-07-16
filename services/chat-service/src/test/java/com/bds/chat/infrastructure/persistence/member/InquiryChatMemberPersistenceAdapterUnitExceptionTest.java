package com.bds.chat.infrastructure.persistence.member;

import com.bds.chat.domain.member.InquiryChatMember;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class InquiryChatMemberPersistenceAdapterUnitExceptionTest {

    @Mock InquiryChatMemberJpaRepository jpaRepository;
    @Mock InquiryChatMemberMapper mapper;
    @Mock EntityManager entityManager;

    @InjectMocks InquiryChatMemberPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adapter, "entityManager", entityManager);
    }

    @Nested
    @DisplayName("findActiveMember 실패 케이스")
    class FindActiveMemberFailTest {

        @Test
        void 탈퇴한_멤버는_빈_Optional을_반환한다() {
            given(jpaRepository.findByRoom_IdAndMemberIdAndDeletedAtIsNull(10L, 5L)).willReturn(Optional.empty());

            Optional<InquiryChatMember> result = adapter.findActiveMember(10L, 5L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllByRoomId 실패 케이스")
    class FindAllByRoomIdFailTest {

        @Test
        void 멤버가_없는_방은_빈_리스트를_반환한다() {
            given(jpaRepository.findByRoom_Id(99L)).willReturn(List.of());

            List<InquiryChatMember> result = adapter.findAllByRoomId(99L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByRoomIdAndMemberId 실패 케이스")
    class FindByRoomIdAndMemberIdFailTest {

        @Test
        void 멤버가_없으면_빈_Optional을_반환한다() {
            given(jpaRepository.findByRoom_IdAndMemberId(10L, 99L)).willReturn(Optional.empty());

            Optional<InquiryChatMember> result = adapter.findByRoomIdAndMemberId(10L, 99L);

            assertThat(result).isEmpty();
        }
    }
}
