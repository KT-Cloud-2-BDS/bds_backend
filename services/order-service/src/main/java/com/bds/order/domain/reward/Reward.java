package com.bds.order.domain.reward;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Reward {

    private Long id;
    private Long fundingId;
    private String name;
    private String description;
    private int limitQty;
    private int remainQty;
    private BadgeType badgeType;
    private Long price;
    private LocalDateTime offerAt;
    private Long shippingCharge;

    public static Reward of(Long id, Long fundingId, String name, String description,
                            int limitQty, int remainQty, BadgeType badgeType,
                            Long price, LocalDateTime offerAt, Long shippingCharge) {
        return new Reward(id, fundingId, name, description, limitQty, remainQty,
                badgeType, price, offerAt, shippingCharge);
    }

    public Long calculateAmount(Integer qty) {
        return price * qty;
    }
    
    public Boolean isStockSufficient(Integer requiredQty) {
        return remainQty > 0 && remainQty >= requiredQty;
    }
}