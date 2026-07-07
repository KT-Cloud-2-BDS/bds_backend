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
    private Integer remainQty;
    private BadgeType badgeType;
    private int price;
    private LocalDateTime offerAt;
    private int shippingCharge;

    public static Reward of(Long id, Long fundingId, String name, String description,
                            int limitQty, Integer remainQty, BadgeType badgeType,
                            int price, LocalDateTime offerAt, int shippingCharge) {
        return new Reward(id, fundingId, name, description, limitQty, remainQty,
                badgeType, price, offerAt, shippingCharge);
    }
}