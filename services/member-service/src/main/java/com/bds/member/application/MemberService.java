package com.bds.member.application;

import com.bds.member.domain.entity.Member;
import com.bds.member.global.exception.BusinessException;
import com.bds.member.global.exception.ErrorCode;
import com.bds.member.infrastructure.persistence.adapter.MemberAdapter;
import com.bds.member.infrastructure.persistence.feignClient.AuthFeignClient;
import com.bds.member.presentation.dto.AuthCreateRequestDto;
import com.bds.member.presentation.dto.AuthLoginRequestDto;
import com.bds.member.presentation.dto.AuthLoginResponseDto;
import com.bds.member.presentation.dto.MemberLoginRequestDto;
import com.bds.member.presentation.dto.MemberSignupRequestDto;
import feign.Response;
import lombok.RequiredArgsConstructor;
import org.hibernate.internal.util.collections.BoundedConcurrentHashMap.EvictionPolicy;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final AuthFeignClient authFeignClient;
    private final MemberAdapter memberAdapter;

    @Transactional
    public void signUp(MemberSignupRequestDto requestDto) {

        if (memberAdapter.existsByNickname(requestDto.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        AuthCreateRequestDto authRequest = new AuthCreateRequestDto(requestDto.email(), requestDto.password());

        Long authId = authFeignClient.createAuthAccount(authRequest).getBody();

        Member newMember = Member.create(
            authId,
            requestDto.nickname()
        );

        memberAdapter.save(newMember);
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
}