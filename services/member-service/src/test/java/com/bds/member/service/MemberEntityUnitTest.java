package com.bds.member.service;

import com.bds.member.domain.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Member 엔티티 단위 테스트 - 성공 케이스")
public class MemberEntityUnitTest {

    @Nested
    @DisplayName("Member.create() 테스트")
    public class CreateMember {

        @Test
        @DisplayName("정상적인 authId와 닉네임이 주어지면 Member 객체가 정상 생성된다")
        public void 회원생성_성공() {
            // given
            Long authId = 24L;
            String nickname = "test";

            // when
            Member member = Member.create(authId, nickname);

            // then
            assertNotNull(member);
            assertEquals(authId, member.getAuthId());
            assertEquals(nickname, member.getNickname());
            assertFalse(member.isDeleted());
        }
    }

    @Nested
    @DisplayName("Member.of() 테스트")
    public class OfMember {

        @Test
        @DisplayName("모든 필드 값이 주어지면 조회용 Member 객체가 정상 매핑된다")
        public void 회원매핑_성공() {
            // given
            Long id = 1L;
            Long authId = 24L;
            String nickname = "test";
            boolean isDeleted = false;

            // when
            Member member = Member.of(id, authId, nickname, isDeleted);

            // then
            assertEquals(id, member.getId());
            assertEquals(authId, member.getAuthId());
            assertEquals(nickname, member.getNickname());
            assertEquals(isDeleted, member.isDeleted());
        }
    }

    @Nested
    @DisplayName("changeNickname() 테스트")
    public class ChangeNickname {

        @Test
        @DisplayName("새로운 정상 닉네임이 주어지면 nickname 필드가 변경된다")
        public void 닉네임변경_성공() {
            // given
            Member member = Member.create(24L, "기존닉네임");
            String newNickname = "새로운닉네임";

            // when
            member.updateNickname(newNickname);

            // then
            assertEquals(newNickname, member.getNickname());
        }
    }
}