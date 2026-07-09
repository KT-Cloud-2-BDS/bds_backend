package com.bds.auth.presentation.controller;

import com.bds.auth.application.AuthService;
import com.bds.auth.presentation.dto.EmailRequestDto;
import com.bds.auth.presentation.dto.VerifyCodeRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auths")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/mail")
    public ResponseEntity<Void> sendSignUpVerificationCode(@RequestBody EmailRequestDto emailRequestDto) {
        authService.sendSignUpVerificationCode(emailRequestDto.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mailCheck")
    public ResponseEntity<String> checkVerificationCode(@RequestBody VerifyCodeRequestDto requestDto) {
        authService.verifyCode(requestDto.email(),requestDto.verificationCode());
        return ResponseEntity.ok("인증이 성공적으로 완료되었습니다.");

    }

}
