package com.bds.auth.presentation.controller;

import com.bds.auth.application.AuthService;
import com.bds.auth.presentation.dto.AuthCreateRequestDto;
import com.bds.auth.presentation.dto.AuthLoginRequestDto;
import com.bds.auth.presentation.dto.AuthLoginResponseDto;
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
public class AuthInternalController {

    private final AuthService authService;

    @PostMapping("/account")
    public ResponseEntity<Long> createAuthAccount(@RequestBody AuthCreateRequestDto requestDto) {
        Long authId = authService.createAccount(requestDto.email(), requestDto.password());
        return ResponseEntity.ok(authId);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthLoginResponseDto> login(@RequestBody AuthLoginRequestDto requestDto) {
        AuthLoginResponseDto responseDto = authService.login(requestDto.email(), requestDto.password());
        return ResponseEntity.ok(responseDto);
    }

    @DeleteMapping("/{authId}")
    public ResponseEntity<Void> deleteAuth(@PathVariable("authId") Long authId) {
        authService.deleteAuth(authId);
        return ResponseEntity.ok().build();
    }
}
