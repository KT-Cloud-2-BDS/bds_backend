package com.bds.member.presentation.controller;

import com.bds.member.application.MemberService;
import com.bds.member.presentation.dto.AuthLoginResponseDto;
import com.bds.member.presentation.dto.MemberLoginRequestDto;
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
        memberService.signUp(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthLoginResponseDto> login(@RequestBody MemberLoginRequestDto requestDto) {
        AuthLoginResponseDto loginResponse = memberService.login(requestDto);
        return ResponseEntity.ok(loginResponse);
    }
}

