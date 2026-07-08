package com.bds.member.domain.entity;

import lombok.Getter;

@Getter
public class Member {

    private final Long id;
    private final Long authId;
    private final String nickname;

    private Member(Long id, Long authId, String nickname) {
        this.id = id;
        this.authId = authId;
        this.nickname = nickname;
    }

    public static Member create(Long authId, String nickname) {
        if (authId == null) throw new IllegalArgumentException("인증 정보가 없습니다.");
        if (nickname == null || nickname.isBlank()) throw new IllegalArgumentException("닉네임은 필수입니다.");
        return new Member(null, authId, nickname);
    }

    public static Member of(Long id, Long authId, String nickname) {
        return new Member(id, authId, nickname);
    }
}
