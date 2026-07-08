package com.bds.member.presentation.controller;

import com.bds.member.application.MemberService;
import com.bds.member.presentation.dto.MemberSignupRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<Void> signUp(@RequestBody MemberSignupRequestDto requestDto) {

        // 우리가 만든 오케스트레이터 서비스 호출!
        memberService.signUp(requestDto);

        // 성공 시 201 Created 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}

