package com.bds.order.application;

import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.domain.order.Order;
import com.bds.order.domain.order.OrderRepository;
import com.bds.order.domain.order.OrderStatus;
import com.bds.order.infrastructure.funding.FundingJpaEntity;
import com.bds.order.infrastructure.funding.FundingJpaRepository;
import com.bds.order.infrastructure.order.OrderMapper;
import com.bds.order.infrastructure.orderReward.OrderRewardJpaEntity;
import com.bds.order.infrastructure.orderReward.OrderRewardJpaRepository;
import com.bds.order.infrastructure.reward.RewardJpaEntity;
import com.bds.order.infrastructure.reward.RewardJpaRepository;
import com.bds.order.presentation.dto.OrderResponseDto;
import com.bds.support.AbstractIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private FundingJpaRepository fundingJpaRepository;

    @Autowired
    private RewardJpaRepository rewardJpaRepository;

    @Autowired
    private OrderRewardJpaRepository orderRewardJpaRepository;
    @Autowired
    private OrderMapper orderMapper;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        FundingJpaEntity funding = new FundingJpaEntity(
                null, "Title", 100L, FundingStatus.ACTIVE,
                now.minusDays(10), now.plusDays(30), now.plusDays(60),
                0, 1000000L, 500000L, false, now, now, new ArrayList<>()
        );
        fundingJpaRepository.save(funding);

        RewardJpaEntity reward = new RewardJpaEntity(
                null, funding, "리워드A", "설명", 100, 100,
                null, 33000L, now.plusDays(60), 3000L
        );
        rewardJpaRepository.save(reward);

        Order order1 = Order.create(1L, 33000L, 3000L, OrderStatus.PENDING);
        Order order2 = Order.create(1L, 53000L, 5000L, OrderStatus.PAID);
        Order savedOrder1 = orderRepository.save(order1);
        Order savedOrder2 = orderRepository.save(order2);

        OrderRewardJpaEntity orderReward1 = new OrderRewardJpaEntity(null, orderMapper.toJpaEntity(savedOrder1), reward, 1, 33000L, 3000L);
        OrderRewardJpaEntity orderReward2 = new OrderRewardJpaEntity(null, orderMapper.toJpaEntity(savedOrder2), reward, 2, 53000L, 5000L);
        orderRewardJpaRepository.save(orderReward1);
        orderRewardJpaRepository.save(orderReward2);
    }

    @AfterEach
    void tearDown() {
        orderRewardJpaRepository.deleteAll();
        orderRepository.deleteAll();
        rewardJpaRepository.deleteAll();
        fundingJpaRepository.deleteAll();
    }

    @Nested
    @DisplayName("주문 목록 조회 정상 테스트")
    class GetAllOrdersTest {

        @Test
        void 회원의_주문_목록을_DB에서_조회한다() {
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 20);

            List<OrderResponseDto> result = orderService.getAllOrders(memberId, pageable);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(OrderResponseDto::orderStatus)
                    .containsExactlyInAnyOrder(OrderStatus.PENDING, OrderStatus.PAID);
            assertThat(result).extracting(OrderResponseDto::title)
                    .containsOnly("Title");
        }
    }
}