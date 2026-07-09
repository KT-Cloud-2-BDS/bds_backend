package com.bds.member.domain.entity;

import com.bds.member.global.exception.BusinessException;
import com.bds.member.global.exception.ErrorCode;
import lombok.Getter;

@Getter
public class Member {

    private final Long id;
    private final Long authId;
    private  String nickname;
    private boolean isDeleted;

    private Member(Long id, Long authId, String nickname, boolean isDeleted) {
        this.id = id;
        this.authId = authId;
        this.nickname = nickname;
        this.isDeleted = isDeleted;
    }

    public static Member create(Long authId, String nickname) {
        if (authId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED_MEMBER);
        if (nickname == null || nickname.isBlank()) throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        return new Member(null, authId, nickname, false);
    }

    public static Member of(Long id, Long authId, String nickname, boolean isDeleted) {
        return new Member(id, authId, nickname, isDeleted);
    }

    public void changeNickname(String newNickname) {
        if (newNickname == null || newNickname.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        this.nickname = newNickname;
    }

}
