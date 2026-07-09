package com.bds.member.service;

import com.bds.member.domain.entity.Member;
import com.bds.member.global.exception.BusinessException;
import com.bds.member.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Member 엔티티 단위 테스트 - 예외 케이스")
public class MemberEntityUnitExceptionTest {

    @Nested
    @DisplayName("Member.create() 예외 핸들링")
    public class CreateMemberException {

        @Test
        @DisplayName("authId가 null이면 UNAUTHORIZED_MEMBER 예외가 터진다")
        public void authId_null_예외발생() {
            // given
            Long authId = null;
            String nickname = "test";

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                Member.create(authId, nickname);
            });

            assertEquals(ErrorCode.UNAUTHORIZED_MEMBER, exception.getErrorCode());
        }

        @Test
        @DisplayName("닉네임이 null이면 예외가 터진다")
        public void 닉네임_null_예외발생() {
            // given
            Long authId = 24L;
            String nickname = null;

            // when & then
            assertThrows(BusinessException.class, () -> {
                Member.create(authId, nickname);
            });
        }

        @Test
        @DisplayName("닉네임이 공백(Blank)이면 예외가 터진다")
        public void 닉네임_공백_예외발생() {
            // given
            Long authId = 24L;
            String nickname = "   ";

            // when & then
            assertThrows(BusinessException.class, () -> {
                Member.create(authId, nickname);
            });
        }
    }

    @Nested
    @DisplayName("changeNickname() 예외 핸들링")
    public class ChangeNicknameException {

        @Test
        @DisplayName("변경하려는 닉네임이 null이거나 공백이면 INVALID_INPUT_VALUE 예외가 터진다")
        public void 변경닉네임_공백_예외발생() {
            // given
            Member member = Member.create(24L, "기존닉네임");
            String invalidNickname = "";

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                member.changeNickname(invalidNickname);
            });
            assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        }
    }
}