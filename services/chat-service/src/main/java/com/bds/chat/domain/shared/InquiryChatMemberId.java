package com.bds.chat.domain.shared;

import com.bds.chat.common.BusinessException;
import com.bds.chat.common.ErrorCode;

import java.util.Objects;

public final class InquiryChatMemberId {
    private final Long value;

    private InquiryChatMemberId(Long value) {
        if (value == null || value <= 0L) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "InquiryChatMemberId must be positive");
        }
        this.value = value;
    }

    public static InquiryChatMemberId of(Long value) {
        return new InquiryChatMemberId(value);
    }

    public Long value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InquiryChatMemberId that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
