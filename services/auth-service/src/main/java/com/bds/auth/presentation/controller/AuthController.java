package com.bds.auth.presentation.controller;

import com.bds.auth.application.AuthService;
import com.bds.auth.domain.entity.enums.Role;
import com.bds.auth.presentation.dto.AuthLoginRequestDto;
import com.bds.auth.presentation.dto.AuthLoginResponseDto;
import com.bds.auth.presentation.dto.AuthRoleResponseDto;
import com.bds.auth.presentation.dto.EmailRequestDto;
import com.bds.auth.presentation.dto.TokenRefreshRequestDto;
import com.bds.auth.presentation.dto.VerifyCodeRequestDto;
import com.bds.common.annotation.LoginUser;
import com.bds.common.dto.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

    @PostMapping("/login")
    public ResponseEntity<AuthLoginResponseDto> login(@RequestBody AuthLoginRequestDto requestDto) {
        AuthLoginResponseDto response = authService.login(requestDto.email(), requestDto.password());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<AuthLoginResponseDto> reissueToken(@RequestBody TokenRefreshRequestDto requestDto) {
        AuthLoginResponseDto response = authService.reissueToken(requestDto.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/role")
    public ResponseEntity<AuthRoleResponseDto> switchRole(@LoginUser CurrentUser user) {
        Role newRole = authService.switchRole(user.id());
        return ResponseEntity.ok(new AuthRoleResponseDto(newRole));
    }

}
