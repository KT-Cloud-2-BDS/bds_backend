package com.bds.chat.domain.shared;

import com.bds.chat.common.BusinessException;
import com.bds.chat.common.ErrorCode;

import java.util.Objects;

public final class ChatRoomId {
    private final Long value;

    private ChatRoomId(Long value) {
        if (value == null || value <= 0L) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "ChatRoomId must be positive");
        }
        this.value = value;
    }

    public static ChatRoomId of(Long value) {
        return new ChatRoomId(value);
    }

    public Long value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatRoomId that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
