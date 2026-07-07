package com.bds.chat.domain.shared;

import com.bds.chat.common.BusinessException;
import com.bds.chat.common.ErrorCode;

import java.util.Objects;

public final class ProductId {
    private final Long value;

    private ProductId(Long value) {
        if (value == null || value <= 0L) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "ProductId must be positive");
        }
        this.value = value;
    }

    public static ProductId of(Long value) {
        return new ProductId(value);
    }

    public Long value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductId that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
