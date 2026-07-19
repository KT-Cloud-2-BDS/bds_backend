package com.bds.member.application;

import com.bds.member.domain.entity.Member;
import com.bds.member.domain.repository.MemberRepository;
import com.bds.member.global.exception.BusinessException;
import org.springframework.dao.DataIntegrityViolationException;
import com.bds.member.global.exception.ErrorCode;
import com.bds.member.infrastructure.persistence.feignClient.AuthFeignClient;
import com.bds.member.presentation.dto.AuthCreateRequestDto;
import com.bds.member.presentation.dto.MemberInfoRequestDto;
import com.bds.member.presentation.dto.MemberResponseDto;
import com.bds.member.presentation.dto.MemberSignupRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
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
        } catch (DataIntegrityViolationException e) {
            try {
                authFeignClient.deleteAuth(authId);
            } catch (Exception ex) {
                log.error("[회원가입 롤백 실패] 닉네임 중복으로 인한 Auth 삭제 실패 - authId: {}", authId, ex);
            }
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);

        } catch (Exception e) {
            try {
                authFeignClient.deleteAuth(authId);
            } catch (Exception ex) {
                log.error("[회원가입 롤백 실패] 일반 예외로 인한 Auth 삭제 실패 - authId: {}", authId, ex);
            }
            throw e;
        }
    }

    @Transactional
    public void completeSocialSignup(Long authId, String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (memberRepository.existsByAuthId(authId)) {
            throw new BusinessException(ErrorCode.ALREADY_REGISTERED_MEMBER);
        }
        if (memberRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        Member newMember = Member.create(authId, nickname);
        memberRepository.save(newMember);
    }

    @Transactional
    public void updateNickname(Long authId, MemberInfoRequestDto requestDto) {
        if (requestDto.nickname() == null || requestDto.nickname().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Member member = memberRepository.findByAuthId(authId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (!member.getNickname().equals(requestDto.nickname())) {
            if (memberRepository.existsByNickname(requestDto.nickname())) {
                throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
            }
        }

        member.updateNickname(requestDto.nickname());
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

    @Transactional(readOnly = true)
    public MemberResponseDto getInfo(Long authId) {
        Member member = memberRepository.findByAuthId(authId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return new MemberResponseDto(member.getNickname());
    }
}