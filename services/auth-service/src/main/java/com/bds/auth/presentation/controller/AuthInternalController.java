package com.bds.auth.presentation.controller;

import com.bds.auth.application.AuthService;
import com.bds.auth.presentation.dto.AuthCreateRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthInternalController {

    private final AuthService authService;

    @PostMapping("/account")
    public ResponseEntity<Long> createAuthAccount(@RequestBody AuthCreateRequestDto requestDto) {
        Long authId = authService.createAccount(requestDto.email(), requestDto.password());
        return ResponseEntity.ok(authId);
    }

}
