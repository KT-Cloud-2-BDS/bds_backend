package com.bds.member.presentation.controller;

import com.bds.common.annotation.LoginUser;
import com.bds.common.dto.CurrentUser;
import com.bds.member.application.MemberService;
import com.bds.member.presentation.dto.MemberInfoRequestDto;
import com.bds.member.presentation.dto.MemberResponseDto;
import com.bds.member.presentation.dto.MemberSignupRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<Void> signUp(@RequestBody MemberSignupRequestDto requestDto) {
        memberService.signUp(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/info")
    public ResponseEntity<Void> updateNickname(
        @LoginUser CurrentUser user,
        @RequestBody MemberInfoRequestDto requestDto
    ) {
        memberService.updateNickname(user.id(), requestDto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteMember(
        @LoginUser CurrentUser user
    ) {
        memberService.deleteMember(user.id());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/info")
    public ResponseEntity<MemberResponseDto> getInfo(
        @LoginUser CurrentUser user
    ) {
        return ResponseEntity.ok(memberService.getInfo(user.id()));
    }
}

