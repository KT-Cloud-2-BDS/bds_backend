package com.bds.order.infrastructure.messaging;

import com.bds.common.events.payment.OrderProcessEvent;
import com.bds.order.application.OrderMessageHandler;
import com.bds.order.application.OrderService;
import com.bds.order.domain.funding.FundingType;
import com.bds.order.domain.order.Order;
import com.bds.order.domain.order.OrderRepository;
import com.bds.order.domain.order.OrderStatus;
import com.bds.order.fixture.BillingFixture;
import com.bds.order.fixture.FundingFixture;
import com.bds.order.fixture.RewardFixture;
import com.bds.order.infrastructure.funding.FundingJpaEntity;
import com.bds.order.infrastructure.funding.FundingJpaRepository;
import com.bds.order.infrastructure.messaging.publisher.NotificationEventPublisher;
import com.bds.order.infrastructure.messaging.publisher.PaymentEventPublisher;
import com.bds.order.infrastructure.orderReward.OrderRewardJpaRepository;
import com.bds.order.infrastructure.reward.RewardJpaEntity;
import com.bds.order.infrastructure.reward.RewardJpaRepository;
import com.bds.order.presentation.dto.BillingResponseDto;
import com.bds.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class OrderMessageHandlerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OrderMessageHandler orderMessageHandler;

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

    @MockitoBean
    private PaymentEventPublisher paymentEventPublisher;

    @MockitoBean
    private NotificationEventPublisher notificationEventPublisher;

    @AfterEach
    void tearDown() {
        orderRewardJpaRepository.deleteAll();
        orderRepository.deleteAll();
        rewardJpaRepository.deleteAll();
        fundingJpaRepository.deleteAll();
    }

    private Long createOrder(OrderStatus status, FundingType type) {
        FundingJpaEntity savedFunding = fundingJpaRepository.save(FundingFixture.createFundingJpaEntity(type));
        RewardJpaEntity savedReward = rewardJpaRepository.save(RewardFixture.createRewardJpaEntity(savedFunding));
        BillingResponseDto billing = orderService.createBilling(
                1L,
                BillingFixture.createBillingRequest(savedFunding.getId(), BillingFixture.rq(savedReward.getId(), 1)));
        Order order = orderRepository.findByIdForUpdate(billing.orderId()).orElseThrow();
        order.updateStatus(status);
        return orderRepository.save(order).getId();
    }

    @Nested
    @DisplayName("processPaid")
    class ProcessPaid {

        @Test
        void 주문을_PAID로_변경한다() {
            Long orderId = createOrder(OrderStatus.PAYING, FundingType.INSTANT);

            orderMessageHandler.processPaid(orderId);

            Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        }
    }

    @Nested
    @DisplayName("processCancelled")
    class ProcessCancelled {

        @Test
        void 주문을_CANCELLED로_변경한다() {
            Long orderId = createOrder(OrderStatus.PAYING, FundingType.INSTANT);

            orderMessageHandler.processCancelled(orderId, "PAYMENT_FAILED");

            Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.getCancelReason()).isEqualTo("PAYMENT_FAILED");
        }
    }

    @Nested
    @DisplayName("processBulk")
    class ProcessBulk {

        @Test
        void CONFIRMED_타입이면_주문을_CONFIRMED로_변경한다() {
            Long orderId = createOrder(OrderStatus.PAYING, FundingType.RESERVED);
            OrderProcessEvent message = OrderProcessEvent.confirmed(List.of(orderId));

            orderMessageHandler.processBulk(message);

            Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }
    }
}
