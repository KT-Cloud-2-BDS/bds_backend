package com.bds.order.application;


import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.domain.funding.FundingType;
import com.bds.order.domain.order.Order;
import com.bds.order.domain.order.OrderRepository;
import com.bds.order.domain.order.OrderStatus;
import com.bds.order.global.exception.BusinessException;
import com.bds.order.infrastructure.funding.FundingJpaEntity;
import com.bds.order.infrastructure.funding.FundingJpaRepository;
import com.bds.order.infrastructure.orderReward.OrderRewardJpaRepository;
import com.bds.order.infrastructure.reward.RewardJpaEntity;
import com.bds.order.infrastructure.reward.RewardJpaRepository;
import com.bds.order.presentation.dto.BillingRequestDto;
import com.bds.order.presentation.dto.BillingResponseDto;
import com.bds.order.presentation.dto.OrderCreateRequestDto;
import com.bds.order.presentation.dto.RewardQuantityDto;
import com.bds.support.AbstractIntegrationTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class OrderServiceIntegrationExceptionTest extends AbstractIntegrationTest {

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

    private FundingJpaEntity savedFunding;
    private FundingJpaEntity expiredFunding;
    private RewardJpaEntity savedReward;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        savedFunding = fundingJpaRepository.save(new FundingJpaEntity(
                null, "테스트 펀딩", 100L, FundingStatus.ACTIVE, FundingType.INSTANT,
                now.minusDays(10), now.plusDays(30), now.plusDays(60),
                0, 1000000L, 500000L, false, new ArrayList<>()
        ));

        expiredFunding = fundingJpaRepository.save(new FundingJpaEntity(
                null, "종료된 펀딩", 101L, FundingStatus.ACTIVE, FundingType.INSTANT,
                now.minusDays(30), now.minusDays(1), now.plusDays(30),
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

    private Long createBillingAndGetOrderId(Long memberId, Long rewardId, int qty) {
        BillingRequestDto reqDto = new BillingRequestDto(savedFunding.getId(), false, List.of(
                new RewardQuantityDto(rewardId, qty)
        ));
        BillingResponseDto billing = orderService.createBilling(memberId, reqDto);
        return billing.orderId();
    }

    private Long createCancelOrderAndGetOrderId() {
        Long orderId = createBillingAndGetOrderId(1L, savedReward.getId(), 1);

        Order savedOrder = orderRepository.findByIdForUpdate(orderId).orElseThrow();
        savedOrder.updateStatus(OrderStatus.PAYING);
        savedOrder.updateStatus(OrderStatus.CANCELLED);
        return orderRepository.save(savedOrder).getId();
    }

    @Nested
    @DisplayName("빌링 생성 예외 통합테스트")
    class CreateBillingExceptionIntegrationTest {

        // 존재하지 않는 펀딩 ID → 예외
        @Test
        void 존재하지_않는_펀딩이면_예외를_던진다() {
            BillingRequestDto reqDto = new BillingRequestDto(999L, false, List.of(
                    new RewardQuantityDto(savedReward.getId(), 1)
            ));

            assertThatThrownBy(() -> orderService.createBilling(1L, reqDto))
                    .isInstanceOf(BusinessException.class);
        }

        // 펀딩 기간 종료 → 예외
        @Test
        void 펀딩_기간이_종료되면_예외를_던진다() {
            BillingRequestDto reqDto = new BillingRequestDto(expiredFunding.getId(), false, List.of(
                    new RewardQuantityDto(savedReward.getId(), 1)
            ));

            assertThatThrownBy(() -> orderService.createBilling(1L, reqDto))
                    .isInstanceOf(BusinessException.class);
        }

        // 존재하지 않는 리워드 ID → 예외
        @Test
        void 존재하지_않는_리워드이면_예외를_던진다() {
            BillingRequestDto reqDto = new BillingRequestDto(savedFunding.getId(), false, List.of(
                    new RewardQuantityDto(999L, 1)
            ));

            assertThatThrownBy(() -> orderService.createBilling(1L, reqDto))
                    .isInstanceOf(BusinessException.class);
        }

        // 재고 부족한 리워드 → 예외
        @Test
        void 빌링_생성_시_재고가_부족하면_예외를_던진다() {
            BillingRequestDto reqDto = new BillingRequestDto(savedFunding.getId(), false, List.of(
                    new RewardQuantityDto(savedReward.getId(), 999)
            ));

            assertThatThrownBy(() -> orderService.createBilling(1L, reqDto))
                    .isInstanceOf(BusinessException.class);
        }

        // 동일 리워드 중복 선택 → 예외
        @Test
        void 동일_리워드를_중복_선택하면_예외를_던진다() {
            BillingRequestDto reqDto = new BillingRequestDto(savedFunding.getId(), false, List.of(
                    new RewardQuantityDto(savedReward.getId(), 1),
                    new RewardQuantityDto(savedReward.getId(), 2)
            ));

            assertThatThrownBy(() -> orderService.createBilling(1L, reqDto))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("주문 생성 예외 통합테스트")
    class CreateOrderExceptionIntegrationTest {


        // 본인 주문 아님 → 예외
        @Test
        void 본인의_주문이_아니면_예외를_던진다() {
            Long orderId = createBillingAndGetOrderId(1L, savedReward.getId(), 1);

            OrderCreateRequestDto reqDto = new OrderCreateRequestDto(
                    orderId, savedFunding.getId(), true
            );

            assertThatThrownBy(() -> orderService.createOrder(999L, reqDto))
                    .isInstanceOf(BusinessException.class);
        }

        // 이미 PAYING 상태 → 상태 전이 불가 예외 (중복 결제 방지)
        @Test
        void 이미_PAYING_상태인_주문은_재결제할_수_없다() {
            Long orderId = createBillingAndGetOrderId(1L, savedReward.getId(), 1);

            OrderCreateRequestDto reqDto = new OrderCreateRequestDto(
                    orderId, savedFunding.getId(), true
            );

            orderService.createOrder(1L, reqDto); // 첫 번째 성공

            assertThatThrownBy(() -> orderService.createOrder(1L, reqDto))
                    .isInstanceOf(BusinessException.class);
        }

        // 재고 부족 → 예외 + 트랜잭션 롤백으로 이전 차감 없음 검증
        @Test
        void 재고_부족_시_예외를_던지고_차감이_롤백된다() {
            // 재고 2개짜리 리워드 생성
            RewardJpaEntity rewardStock2 = rewardJpaRepository.save(new RewardJpaEntity(
                    null, savedFunding, "재고2리워드", "설명", 100, 2,
                    null, 10000L, LocalDateTime.now().plusDays(60), 3000L
            ));

            // 회원A: 빌링 생성 (2개 요청, 재고 2개이므로 통과)
            Long orderIdA = createBillingAndGetOrderId(1L, rewardStock2.getId(), 2);

            // 회원B: 빌링 생성 + 주문 생성 (1개 차감) → 잔여 재고 1개
            Long orderIdB = createBillingAndGetOrderId(2L, rewardStock2.getId(), 1);
            OrderCreateRequestDto reqDtoB = new OrderCreateRequestDto(
                    orderIdB, savedFunding.getId(), true
            );
            orderService.createOrder(2L, reqDtoB);

            // 회원A: 주문 생성 시도 (2개 필요한데 잔여 1개 → 실패)
            OrderCreateRequestDto reqDtoA = new OrderCreateRequestDto(
                    orderIdA, savedFunding.getId(), true
            );

            assertThatThrownBy(() -> orderService.createOrder(1L, reqDtoA))
                    .isInstanceOf(BusinessException.class);

            // 회원B의 차감만 반영, 회원A는 롤백되어 재고 1 유지
            RewardJpaEntity reward = rewardJpaRepository.findById(rewardStock2.getId()).orElseThrow();
            assertThat(reward.getRemainQty()).isEqualTo(1);
        }

        // 존재하지 않는 orderId → 예외
        @Test
        void 존재하지_않는_주문이면_예외를_던진다() {
            OrderCreateRequestDto reqDto = new OrderCreateRequestDto(
                    999L, savedFunding.getId(), true
            );

            assertThatThrownBy(() -> orderService.createOrder(1L, reqDto))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("주문 취소 예외 통합테스트")
    class CancelOrderExceptionIntegrationTest {

        private Long createAndStartOrder(Long memberId) {
            BillingRequestDto billingReqDto = new BillingRequestDto(savedFunding.getId(), false, List.of(
                    new RewardQuantityDto(savedReward.getId(), 1)
            ));
            BillingResponseDto billing = orderService.createBilling(memberId, billingReqDto);

            OrderCreateRequestDto createReqDto = new OrderCreateRequestDto(
                    billing.orderId(), savedFunding.getId(), true
            );
            orderService.createOrder(memberId, createReqDto);

            return billing.orderId();
        }

        // 본인 주문 아님 → 예외
        @Test
        void 본인의_주문이_아니면_취소할_수_없다() {
            Long orderId = createAndStartOrder(1L);

            assertThatThrownBy(() -> orderService.cancelOrder(999L, orderId))
                    .isInstanceOf(BusinessException.class);
        }

        // PENDING 상태 주문 취소 → 상태 전이 불가 예외
        @Test
        void PENDING_상태에서는_취소할_수_없다() {
            BillingRequestDto billingReqDto = new BillingRequestDto(savedFunding.getId(), false, List.of(
                    new RewardQuantityDto(savedReward.getId(), 1)
            ));
            BillingResponseDto billing = orderService.createBilling(1L, billingReqDto);

            assertThatThrownBy(() -> orderService.cancelOrder(1L, billing.orderId()))
                    .isInstanceOf(BusinessException.class);
        }

        // 이미 취소된 주문 재취소 → 예외
        @Test
        void 이미_취소된_주문은_다시_취소할_수_없다() {
            Long orderId = createAndStartOrder(1L);
            orderService.cancelOrder(1L, orderId);

            assertThatThrownBy(() -> orderService.cancelOrder(1L, orderId))
                    .isInstanceOf(BusinessException.class);
        }

        // 존재하지 않는 주문 상세 조회 → 예외
        @Test
        void 존재하지_않는_주문_상세_조회_시_예외를_던진다() {
            assertThatThrownBy(() -> orderService.getOrderDetail(1L, 999L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("processStatusUpdate 예외 통합테스트")
    class ProcessStatusUpdateExceptionIntegrationTest {


        @Test
        void 존재하지_않는_주문이면_예외_없이_warn_로그를_남긴다(CapturedOutput output) {
            orderService.processStatusUpdate(9999L, OrderStatus.PAYING);

            assertThat(output.getOut()).contains("Order not found: orderId=9999");
        }

        @Transactional
        @Test
        void 상태_전이_불가능하면_예외_없이_warn_로그를_남긴다(CapturedOutput output) {
            Long orderId = createCancelOrderAndGetOrderId();

            orderService.processStatusUpdate(orderId, OrderStatus.PAYING);

            assertThat(output.getOut()).contains("[OrderService] processStatusUpdate failed - invalid state: orderId=" + orderId);
        }
    }

    @Nested
    @DisplayName("processCancelledUpdate 예외 통합테스트")
    class ProcessCancelledUpdateExceptionIntegrationTest {

        @Test
        void 존재하지_않는_주문이면_예외_없이_warn_로그를_남긴다(CapturedOutput output) {
            orderService.processCancelledUpdate(9999L, "FUNDING_FAILED");

            assertThat(output.getOut()).contains("Order not found: orderId=9999");
        }

        @Transactional
        @Test
        void 상태_전이_불가능하면_예외_없이_warn_로그를_남긴다(CapturedOutput output) {
            Long orderId = createCancelOrderAndGetOrderId();

            orderService.processCancelledUpdate(orderId, "FUNDING_FAILED");

            assertThat(output.getOut()).contains("[OrderService] processCancelledUpdate failed - invalid state: orderId=" + orderId);
        }
    }

    @Nested
    @DisplayName("processReservedFundingConfirmed 예외 통합테스트")
    class ProcessPayingAndPublishSettlementExceptionIntegrationTest {

        @Test
        void 존재하지_않는_주문이면_예외_없이_warn_로그를_남긴다(CapturedOutput output) {
            orderService.processReservedFundingConfirmed(9999L);

            assertThat(output.getOut()).contains("Order not found: orderId=9999");
        }

        @Transactional
        @Test
        void 상태_전이_불가능하면_예외_없이_warn_로그를_남긴다(CapturedOutput output) {
            Long orderId = createCancelOrderAndGetOrderId();

            orderService.processReservedFundingConfirmed(orderId);

            assertThat(output.getOut()).contains("[OrderService] processReservedFundingConfirmed failed - invalid state: orderId=" + orderId);
        }
    }

    @Nested
    @DisplayName("processFundingFailedRefund 예외 통합테스트")
    class processFundingFailedRefundExceptionIntegrationTest {

        @Test
        void 존재하지_않는_주문이면_예외_없이_warn_로그를_남긴다(CapturedOutput output) {
            orderService.processFundingFailedRefund(9999L);

            assertThat(output.getOut()).contains("Order not found: orderId=9999");
        }

        @Transactional
        @Test
        void 상태_전이_불가능하면_예외_없이_warn_로그를_남긴다(CapturedOutput output) {
            Long orderId = createCancelOrderAndGetOrderId();

            orderService.processFundingFailedRefund(orderId);

            assertThat(output.getOut()).contains("[OrderService] processFundingFailedRefund failed - invalid state: orderId=" + orderId);
        }
    }

}