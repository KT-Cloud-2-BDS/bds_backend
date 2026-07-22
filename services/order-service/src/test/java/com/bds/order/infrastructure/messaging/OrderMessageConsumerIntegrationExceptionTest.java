package com.bds.order.infrastructure.messaging;

import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.domain.funding.FundingType;
import com.bds.order.domain.order.Order;
import com.bds.order.domain.order.OrderRepository;
import com.bds.order.domain.order.OrderStatus;
import com.bds.order.infrastructure.config.OrderQueues;
import com.bds.order.infrastructure.funding.FundingJpaEntity;
import com.bds.order.infrastructure.funding.FundingJpaRepository;
import com.bds.order.infrastructure.messaging.dto.PaymentCancelledMessage;
import com.bds.order.infrastructure.messaging.dto.PaymentPaidMessage;
import com.bds.order.infrastructure.messaging.dto.PaymentProcessedMessage;
import com.bds.order.infrastructure.order.OrderJpaEntity;
import com.bds.order.infrastructure.order.OrderJpaRepository;
import com.bds.order.infrastructure.order.OrderMapper;
import com.bds.order.infrastructure.orderReward.OrderRewardJpaEntity;
import com.bds.order.infrastructure.orderReward.OrderRewardJpaRepository;
import com.bds.order.infrastructure.reward.RewardJpaEntity;
import com.bds.order.infrastructure.reward.RewardJpaRepository;
import com.bds.support.AbstractRabbitMQIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class OrderMessageConsumerIntegrationExceptionTest extends AbstractRabbitMQIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private FundingJpaRepository fundingJpaRepository;

    @Autowired
    private RewardJpaRepository rewardJpaRepository;

    @Autowired
    private OrderRewardJpaRepository orderRewardJpaRepository;

    private FundingJpaEntity savedFunding;
    private RewardJpaEntity savedReward;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        savedFunding = fundingJpaRepository.save(new FundingJpaEntity(
                null, "테스트 펀딩", 100L, FundingStatus.ACTIVE, FundingType.INSTANT,
                now.minusDays(10), now.plusDays(30), now.plusDays(60),
                0, 1000000L, 500000L, false, new ArrayList<>()
        ));

        savedReward = rewardJpaRepository.save(new RewardJpaEntity(
                null, savedFunding, "리워드A", "설명A", 100, 10,
                null, 10000L, now.plusDays(60), 3000L
        ));
    }

    @AfterEach
    void tearDown() {
        orderRewardJpaRepository.deleteAll();
        orderRepository.deleteAll();
        rewardJpaRepository.deleteAll();
        fundingJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("존재하지 않는 orderId로 CANCELLED 메시지 수신 시 에러 메시지를 발행한다")
    void receiveCancelMessage_orderNotFound_publishesError() {
        Long nonExistentOrderId = 9999L;

        rabbitTemplate.convertAndSend(
                OrderQueues.PAYMENT_EXCHANGE,
                "payment.refunded",
                new PaymentCancelledMessage(nonExistentOrderId, "PAYMENT_CANCELLED")
        );

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(orderRepository.findById(nonExistentOrderId)).isEmpty();
        });
    }

    @Test
    @DisplayName("존재하지 않는 orderId로 PAID 메시지 수신 시 에러 메시지를 발행한다")
    void receivePaidMessage_orderNotFound_publishesError() {
        Long nonExistentOrderId = 9999L;

        rabbitTemplate.convertAndSend(
                OrderQueues.PAYMENT_EXCHANGE,
                "payment.paid",
                new PaymentPaidMessage(nonExistentOrderId)
        );

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(orderRepository.findById(nonExistentOrderId)).isEmpty();
        });
        // TODO: error 메시지 발행 검증
    }

    @Test
    @DisplayName("전이 불가능한 상태에서 CANCELLED 메시지 수신 시 상태가 변경되지 않는다")
    void receiveCancelMessage_invalidTransition_statusRemainsUnchanged() {
        Order order = createOrderWithStatus(OrderStatus.REFUNDED);

        rabbitTemplate.convertAndSend(
                OrderQueues.PAYMENT_EXCHANGE,
                "payment.refunded",
                new PaymentCancelledMessage(order.getId(), "PAYMENT_CANCELLED")
        );

        await().during(2, TimeUnit.SECONDS).untilAsserted(() -> {
            Order updated = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        });
    }

    @Test
    @DisplayName("전이 불가능한 상태에서 PAID 메시지 수신 시 상태가 변경되지 않는다")
    void receivePaidMessage_invalidTransition_statusRemainsUnchanged() {
        Order order = createOrderWithStatus(OrderStatus.CANCELLED);

        rabbitTemplate.convertAndSend(
                OrderQueues.PAYMENT_EXCHANGE,
                "payment.paid",
                new PaymentPaidMessage(order.getId())
        );

        await().during(2, TimeUnit.SECONDS).untilAsserted(() -> {
            Order updated = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        });
    }

    @Test
    @DisplayName("전이 불가능한 상태에서 CONFIRMED 벌크 메시지 수신 시 상태가 변경되지 않는다")
    void receiveBulkConfirmedMessage_invalidTransition_statusRemainsUnchanged() {
        Order order = createOrderWithStatus(OrderStatus.CANCELLED);

        rabbitTemplate.convertAndSend(
                OrderQueues.PAYMENT_EXCHANGE,
                "payment.settled",
                new PaymentProcessedMessage(List.of(order.getId()), PaymentProcessedMessage.ResultType.CONFIRMED)
        );

        await().during(2, TimeUnit.SECONDS).untilAsserted(() -> {
            Order updated = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        });
    }

    @Test
    @DisplayName("벌크 메시지에 존재하지 않는 orderId가 포함되어도 나머지는 정상 처리된다")
    void receiveBulkMessage_partialNotFound_remainingProcessed() {
        Order validOrder = createOrderWithStatus(OrderStatus.PAID);
        Long nonExistentOrderId = 9999L;

        rabbitTemplate.convertAndSend(
                OrderQueues.PAYMENT_EXCHANGE,
                "payment.settled",
                new PaymentProcessedMessage(
                        List.of(validOrder.getId(), nonExistentOrderId),
                        PaymentProcessedMessage.ResultType.CONFIRMED)
        );

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Order updated = orderRepository.findById(validOrder.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        });
    }

    @Test
    @DisplayName("벌크 메시지에 전이 불가능한 건이 포함되어도 나머지는 정상 처리된다")
    void receiveBulkMessage_partialInvalidTransition_remainingProcessed() {
        Order validOrder = createOrderWithStatus(OrderStatus.PAID);
        Order invalidOrder = createOrderWithStatus(OrderStatus.CANCELLED);

        rabbitTemplate.convertAndSend(
                OrderQueues.PAYMENT_EXCHANGE,
                "payment.settled",
                new PaymentProcessedMessage(
                        List.of(validOrder.getId(), invalidOrder.getId()),
                        PaymentProcessedMessage.ResultType.CONFIRMED)
        );

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Order updatedValid = orderRepository.findById(validOrder.getId()).orElseThrow();
            Order updatedInvalid = orderRepository.findById(invalidOrder.getId()).orElseThrow();
            assertThat(updatedValid.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(updatedInvalid.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        });
    }

    private Order createOrderWithStatus(OrderStatus status) {
        OrderJpaEntity orderEntity = OrderJpaEntity.builder()
                .orderNo("ORD-TEST-" + System.nanoTime())
                .memberId(1L)
                .status(status)
                .totalRewardAmount(10000L)
                .totalShippingCharge(3000L)
                .build();

        OrderRewardJpaEntity orderRewardEntity = new OrderRewardJpaEntity(
                null, orderEntity, savedReward, 1, 10000L, 3000L
        );
        orderEntity.getOrderRewards().add(orderRewardEntity);

        OrderJpaEntity saved = orderJpaRepository.saveAndFlush(orderEntity);
        return orderMapper.toDomain(saved);
    }
}

