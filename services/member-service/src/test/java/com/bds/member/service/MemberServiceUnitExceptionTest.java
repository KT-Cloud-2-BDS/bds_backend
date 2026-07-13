package com.bds.member.service;

import com.bds.member.application.MemberService;
import com.bds.member.domain.entity.Member;
import com.bds.member.domain.repository.MemberRepository;
import com.bds.member.global.exception.BusinessException;
import com.bds.member.global.exception.ErrorCode;
import com.bds.member.infrastructure.persistence.feignClient.AuthFeignClient;
import com.bds.member.presentation.dto.MemberInfoRequestDto;
import com.bds.member.presentation.dto.MemberSignupRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService 단위 테스트 - 예외 케이스")
public class MemberServiceUnitExceptionTest {

    @InjectMocks
    public MemberService memberService;

    @Mock
    public MemberRepository memberRepository;

    @Mock
    public AuthFeignClient authFeignClient;

    @Nested
    @DisplayName("회원가입 예외 케이스")
    public class SignUpException {

        @Test
        @DisplayName("회원가입 중 DataIntegrityViolationException이 아닌 일반 예외가 발생해도 Auth 계정을 롤백 요청하고 예외를 그대로 던진다")
        public void 회원가입_일반예외_발생시_롤백_검증() {
            // given
            MemberSignupRequestDto requestDto = new MemberSignupRequestDto("yeojin@email.com", "password123!", "BBandiz");
            Long authId = 100L;

            given(memberRepository.existsByNickname(requestDto.nickname())).willReturn(false);

            ResponseEntity<Long> responseEntity = ResponseEntity.ok(authId);
            given(authFeignClient.createAuthAccount(any())).willReturn(responseEntity);

            doThrow(new RuntimeException("DB 연결 끊김")).when(memberRepository).save(any(Member.class));

            // when & then
            assertThrows(RuntimeException.class, () -> {
                memberService.signUp(requestDto);
            });

            verify(authFeignClient, times(1)).deleteAuth(authId);
        }

        @Test
        @DisplayName("찰나의 순간에 닉네임이 중복되어 DataIntegrityViolationException이 발생하면 DUPLICATE_NICKNAME 예외를 던지고 Auth 계정을 롤백(삭제) 요청한다")
        public void 회원가입_닉네임중복_DB충돌_예외() {
            // given
            MemberSignupRequestDto requestDto = new MemberSignupRequestDto("yeojin@email.com", "password123!", "BBandiz");
            Long authId = 100L;

            given(memberRepository.existsByNickname(requestDto.nickname())).willReturn(false);

            ResponseEntity<Long> responseEntity = ResponseEntity.ok(authId);
            given(authFeignClient.createAuthAccount(any())).willReturn(responseEntity);

            doThrow(DataIntegrityViolationException.class).when(memberRepository).save(any(Member.class));

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                memberService.signUp(requestDto);
            });

            assertEquals(ErrorCode.DUPLICATE_NICKNAME, exception.getErrorCode());
            verify(authFeignClient, times(1)).deleteAuth(authId);
        }

        @Test
        @DisplayName("롤백 탈퇴(deleteAuth) 요청 중 외부 서버 장애가 발생하더라도 원래의 닉네임 중복 예외가 정상적으로 전파된다")
        public void 회원가입_롤백실패시에도_원본예외_유지_검증() {
            // given
            MemberSignupRequestDto requestDto = new MemberSignupRequestDto("yeojin@email.com", "password123!", "BBandiz");
            Long authId = 100L;

            given(memberRepository.existsByNickname(requestDto.nickname())).willReturn(false);

            ResponseEntity<Long> responseEntity = ResponseEntity.ok(authId);
            given(authFeignClient.createAuthAccount(any())).willReturn(responseEntity);

            doThrow(DataIntegrityViolationException.class).when(memberRepository).save(any(Member.class));

            doThrow(new RuntimeException("Auth 서버 장애")).when(authFeignClient).deleteAuth(authId);

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                memberService.signUp(requestDto);
            });

            assertEquals(ErrorCode.DUPLICATE_NICKNAME, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("닉네임 수정 예외 핸들링")
    public class UpdateNicknameException {

        @Test
        @DisplayName("수정할 닉네임이 null이거나 공백이면 INVALID_INPUT_VALUE 예외가 터진다")
        public void 닉네임_공백_예외() {
            // given
            Long authId = 24L;
            MemberInfoRequestDto requestDto = new MemberInfoRequestDto(" ");

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                memberService.updateNickname(authId, requestDto);
            });
            assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        }

        @Test
        @DisplayName("수정하려는 닉네임이 이미 사용 중이면 DUPLICATE_NICKNAME 예외가 터진다")
        public void 닉네임_중복_예외() {
            // given
            Long authId = 24L;
            String newNickname = "이미있는닉네임";
            MemberInfoRequestDto requestDto = new MemberInfoRequestDto(newNickname);

            Member mockMember = Member.create(authId, "기존닉네임");
            given(memberRepository.findByAuthId(authId)).willReturn(Optional.of(mockMember));

            given(memberRepository.existsByNickname(newNickname)).willReturn(true);

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                memberService.updateNickname(authId, requestDto);
            });
            assertEquals(ErrorCode.DUPLICATE_NICKNAME, exception.getErrorCode());
        }

        @Test
        @DisplayName("회원 엔티티 정보가 조회되지 않으면 MEMBER_NOT_FOUND 예외가 터진다")
        public void 회원_정보없음_예외() {
            // given
            Long authId = 999L;
            MemberInfoRequestDto requestDto = new MemberInfoRequestDto("새닉네임");

            given(memberRepository.findByAuthId(authId)).willReturn(Optional.empty());

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                memberService.updateNickname(authId, requestDto);
            });
            assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("회원 탈퇴 예외 핸들링")
    public class DeleteMemberException {

        @Test
        @DisplayName("가입된 회원이 존재하지 않으면 MEMBER_NOT_FOUND 예외가 터진다")
        public void 가입된회원_없음_예외발생() {
            // given
            Long authId = 999L;
            given(memberRepository.existsByAuthId(authId)).willReturn(false);

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                memberService.deleteMember(authId);
            });
            assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("Auth 서버 통신이 실패(500)하면 AUTH_SERVICE_ERROR 예외가 터진다")
        public void 인증서버_통신실패_예외발생() {
            // given
            Long authId = 24L;

            given(memberRepository.existsByAuthId(authId)).willReturn(true);
            given(authFeignClient.deleteAuth(authId)).willReturn(ResponseEntity.internalServerError().build());

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                memberService.deleteMember(authId);
            });
            assertEquals(ErrorCode.AUTH_SERVICE_ERROR, exception.getErrorCode());
        }
    }
}