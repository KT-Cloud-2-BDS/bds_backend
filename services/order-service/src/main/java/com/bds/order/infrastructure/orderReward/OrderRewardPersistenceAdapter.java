package com.bds.order.infrastructure.orderReward;

import com.bds.order.domain.orderReward.OrderRewardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderRewardPersistenceAdapter implements OrderRewardRepository {

    private final OrderRewardJpaRepository orderRewardJpaRepository;
    private final OrderRewardMapper orderRewardMapper;

    @Override
    public List<OrderRewardDetailProjection> findOrderRewardDetailsWithReward(Long orderId) {
        return orderRewardJpaRepository.findOrderRewardDetailsWithReward(orderId);
    }
}