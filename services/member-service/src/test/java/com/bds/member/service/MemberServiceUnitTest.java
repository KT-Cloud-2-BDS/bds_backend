package com.bds.member.service;

import com.bds.member.application.MemberService;
import com.bds.member.domain.entity.Member;
import com.bds.member.domain.repository.MemberRepository;
import com.bds.member.infrastructure.persistence.feignClient.AuthFeignClient;
import com.bds.member.presentation.dto.AuthCreateRequestDto;
import com.bds.member.presentation.dto.MemberInfoRequestDto;
import com.bds.member.presentation.dto.MemberResponseDto;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
            Long authId = 1L;
            String newNickname = "newBBandiz";
            MemberInfoRequestDto requestDto = new MemberInfoRequestDto(newNickname);

            Member mockMember = Member.create(authId, "oldNickname");

            given(memberRepository.findByAuthId(authId)).willReturn(Optional.of(mockMember));

            given(memberRepository.existsByNickname(newNickname)).willReturn(false);

            // when
            memberService.updateNickname(authId, requestDto);

            // then
            assertEquals(newNickname, mockMember.getNickname());

            verify(memberRepository, times(1)).save(mockMember);
        }
    }

    @Test
    @DisplayName("현재 본인의 닉네임과 동일한 닉네임으로 수정을 요청하면, 중복 검사를 건너뛰고 정상 처리된다(No-Op)")
    public void 닉네임수정_동일닉네임_성공() {
        // given
        Long authId = 1L;
        String sameNickname = "BBandiz";
        MemberInfoRequestDto requestDto = new MemberInfoRequestDto(sameNickname);

        Member mockMember = Member.create(authId, sameNickname);
        given(memberRepository.findByAuthId(authId)).willReturn(Optional.of(mockMember));

        // when
        memberService.updateNickname(authId, requestDto);

        // then
        assertEquals(sameNickname, mockMember.getNickname());
        verify(memberRepository, times(1)).save(mockMember);

        verify(memberRepository, never()).existsByNickname(anyString());
    }

    @Nested
    @DisplayName("정보 조회 기능")
    public class GetInfo {
        @Test
        @DisplayName("가입된 회원이면 닉네임이 담긴 정보를 반환한다")
        public void 정보조회_성공() {
            // given
            Long authId = 24L;
            Member mockMember = Member.create(authId, "BBandiz");

            given(memberRepository.findByAuthId(authId)).willReturn(Optional.of(mockMember));

            // when
            MemberResponseDto response = memberService.getInfo(authId);

            // then
            assertEquals("BBandiz", response.nickname());
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