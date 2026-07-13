package com.bds.member.service;

import com.bds.member.application.MemberService;
import com.bds.member.domain.entity.Member;
import com.bds.member.domain.repository.MemberRepository;
import com.bds.member.infrastructure.persistence.feignClient.AuthFeignClient;
import com.bds.member.presentation.dto.AuthCreateRequestDto;
import com.bds.member.presentation.dto.MemberInfoRequestDto;
import com.bds.member.presentation.dto.MemberSignupRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService 단위 테스트 - 성공 케이스")
public class MemberServiceUnitTest {

    @InjectMocks
    public MemberService memberService;

    @Mock
    public MemberRepository memberRepository;

    @Mock
    public AuthFeignClient authFeignClient;

    @Nested
    @DisplayName("회원가입 기능")
    public class SignUp {
        @Test
        @DisplayName("닉네임 중복이 없고 Auth 서버 계정 생성이 성공하면 회원가입이 완료된다")
        public void 회원가입_성공() {
            // given
            MemberSignupRequestDto requestDto = new MemberSignupRequestDto("test@email.com", "password123!", "여진닉네임");
            Long mockedAuthId = 100L;

            given(memberRepository.existsByNickname(anyString())).willReturn(false);
            given(authFeignClient.createAuthAccount(any(AuthCreateRequestDto.class)))
                .willReturn(ResponseEntity.ok(mockedAuthId));

            // when
            memberService.signUp(requestDto);

            // then
            verify(memberRepository, times(1)).save(any(Member.class));
        }
    }

    @Nested
    @DisplayName("닉네임 수정 기능")
    public class UpdateNickname {
        @Test
        @DisplayName("정상적인 닉네임 입력이고 중복이 없으면 닉네임이 정상 변경된다")
        public void 닉네임수정_성공() {
            // given
            Long authId = 24L;
            MemberInfoRequestDto requestDto = new MemberInfoRequestDto("새로운닉네임");
            Member mockMember = mock(Member.class);

            given(memberRepository.existsByNickname(anyString())).willReturn(false);
            given(memberRepository.findByAuthId(anyLong())).willReturn(Optional.of(mockMember));

            // when
            memberService.updateNickname(authId, requestDto);

            // then
            verify(mockMember, times(1)).changeNickname(anyString());
            verify(memberRepository, times(1)).save(any(Member.class));
        }
    }

    @Nested
    @DisplayName("회원 탈퇴 기능")
    public class DeleteMember {
        @Test
        @DisplayName("유저가 존재하고 Auth 서버 통신이 성공하면 소프트 딜리트가 정상 수행된다")
        public void 유저존재_통신성공_소프트딜리트수행() {
            // given
            Long authId = 24L;

            given(memberRepository.existsByAuthId(anyLong())).willReturn(true);
            given(authFeignClient.deleteAuth(anyLong())).willReturn(ResponseEntity.ok().build());

            // when
            memberService.deleteMember(authId);

            // then
            verify(memberRepository, times(1)).softDeleteByAuthId(anyLong());
        }
    }
}