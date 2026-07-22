package com.bds.order.infrastructure.messaging;

import com.bds.common.events.payment.OrderProcessEvent;
import com.bds.order.application.OrderMessageHandler;
import com.bds.order.application.OrderService;
import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.domain.funding.FundingType;
import com.bds.order.domain.order.Order;
import com.bds.order.domain.order.OrderRepository;
import com.bds.order.domain.order.OrderStatus;
import com.bds.order.infrastructure.funding.FundingJpaEntity;
import com.bds.order.infrastructure.funding.FundingJpaRepository;
import com.bds.order.infrastructure.messaging.publisher.NotificationEventPublisher;
import com.bds.order.infrastructure.messaging.publisher.PaymentEventPublisher;
import com.bds.order.infrastructure.orderReward.OrderRewardJpaRepository;
import com.bds.order.infrastructure.reward.RewardJpaEntity;
import com.bds.order.infrastructure.reward.RewardJpaRepository;
import com.bds.order.presentation.dto.BillingRequestDto;
import com.bds.order.presentation.dto.BillingResponseDto;
import com.bds.order.presentation.dto.RewardQuantityDto;
import com.bds.support.AbstractIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    private Long createPayingOrder() {
        BillingRequestDto reqDto = new BillingRequestDto(savedFunding.getId(), false, List.of(
                new RewardQuantityDto(savedReward.getId(), 1)
        ));
        BillingResponseDto billing = orderService.createBilling(1L, reqDto);
        Order order = orderRepository.findByIdForUpdate(billing.orderId()).orElseThrow();
        order.updateStatus(OrderStatus.PAYING);
        return orderRepository.save(order).getId();
    }

    @Nested
    @DisplayName("processPaid")
    class ProcessPaid {

        @Test
        void 주문을_PAID로_변경한다() {
            Long orderId = createPayingOrder();

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
            Long orderId = createPayingOrder();

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
            Long orderId = createPayingOrder();
            OrderProcessEvent message = OrderProcessEvent.confirmed(List.of(orderId));

            orderMessageHandler.processBulk(message);

            Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }
    }
}
