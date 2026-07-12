package com.bds.member.application;

import com.bds.member.domain.entity.Member;
import com.bds.member.domain.repository.MemberRepository;
import com.bds.member.global.exception.BusinessException;
import com.bds.member.global.exception.ErrorCode;
import com.bds.member.infrastructure.persistence.feignClient.AuthFeignClient;
import com.bds.member.presentation.dto.AuthCreateRequestDto;
import com.bds.member.presentation.dto.AuthLoginRequestDto;
import com.bds.member.presentation.dto.AuthLoginResponseDto;
import com.bds.member.presentation.dto.MemberLoginRequestDto;
import com.bds.member.presentation.dto.MemberInfoRequestDto;
import com.bds.member.presentation.dto.MemberSignupRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final AuthFeignClient authFeignClient;
    private final MemberRepository memberRepository;

    @Transactional
    public void signUp(MemberSignupRequestDto requestDto) {
        if (memberRepository.existsByNickname(requestDto.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        AuthCreateRequestDto authRequest = new AuthCreateRequestDto(requestDto.email(), requestDto.password());
        Long authId = authFeignClient.createAuthAccount(authRequest).getBody();

        Member newMember = Member.create(authId, requestDto.nickname());

        try {
            memberRepository.save(newMember);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            authFeignClient.deleteAuth(authId);
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        } catch (Exception e) {
            authFeignClient.deleteAuth(authId);
            throw e;
        }
    }

    @Transactional
    public AuthLoginResponseDto login(MemberLoginRequestDto requestDto) {
        AuthLoginRequestDto authRequest = new AuthLoginRequestDto(
            requestDto.email(),
            requestDto.password()
        );

        ResponseEntity<AuthLoginResponseDto> feignResponse = authFeignClient.login(authRequest);

        if (!feignResponse.getStatusCode().is2xxSuccessful() || feignResponse.getBody() == null) {
            throw new BusinessException(ErrorCode.AUTH_SERVICE_ERROR);
        }

        return feignResponse.getBody();
    }

    @Transactional
    public void updateNickname(Long authId, MemberInfoRequestDto requestDto) {
        if (requestDto.nickname() == null || requestDto.nickname().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (memberRepository.existsByNickname(requestDto.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        Member member = memberRepository.findByAuthId(authId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        member.changeNickname(requestDto.nickname());
        memberRepository.save(member);
    }

    @Transactional
    public void deleteMember(Long authId) {
        if (!memberRepository.existsByAuthId(authId)) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        memberRepository.softDeleteByAuthId(authId);

        ResponseEntity<Void> response = authFeignClient.deleteAuth(authId);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new BusinessException(ErrorCode.AUTH_SERVICE_ERROR);
        }
    }
}