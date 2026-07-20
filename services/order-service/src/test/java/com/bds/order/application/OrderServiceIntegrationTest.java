package com.bds.order.application;


import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.domain.funding.FundingType;
import com.bds.order.domain.order.Order;
import com.bds.order.domain.order.OrderRepository;
import com.bds.order.domain.order.OrderStatus;
import com.bds.order.infrastructure.funding.FundingJpaEntity;
import com.bds.order.infrastructure.funding.FundingJpaRepository;
import com.bds.order.infrastructure.order.OrderJpaEntity;
import com.bds.order.infrastructure.order.OrderJpaRepository;
import com.bds.order.infrastructure.order.OrderMapper;
import com.bds.order.infrastructure.orderReward.OrderRewardJpaEntity;
import com.bds.order.infrastructure.orderReward.OrderRewardJpaRepository;
import com.bds.order.infrastructure.reward.RewardJpaEntity;
import com.bds.order.infrastructure.reward.RewardJpaRepository;
import com.bds.order.presentation.dto.*;
import com.bds.support.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OrderService orderService;

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

    @Autowired
    private EntityManager entityManager;

    private FundingJpaEntity savedFunding;
    private RewardJpaEntity savedReward;
    private RewardJpaEntity savedReward2;

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

        savedReward2 = rewardJpaRepository.save(new RewardJpaEntity(
                null, savedFunding, "리워드B", "설명B", 50, 5,
                null, 20000L, now.plusDays(60), 5000L
        ));
    }

    @AfterEach
    void tearDown() {
        orderRewardJpaRepository.deleteAll();
        orderRepository.deleteAll();
        rewardJpaRepository.deleteAll();
        fundingJpaRepository.deleteAll();
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

    @Nested
    @DisplayName("빌링 생성 통합테스트")
    class CreateBillingIntegrationTest {

        // 리워드 1개 선택 → 빌링 생성 → Order/OrderReward 저장 + 금액 정합성 검증
        @Test
        void 리워드_1개로_빌링_생성_시_금액이_정확히_저장된다() {
            BillingRequestDto reqDto = new BillingRequestDto(savedFunding.getId(), false, List.of(
                    new RewardQuantityDto(savedReward.getId(), 2)
            ));

            BillingResponseDto result = orderService.createBilling(1L, reqDto);

            assertThat(result.orderId()).isNotNull();
            assertThat(result.memberId()).isEqualTo(1L);
            assertThat(result.rewards()).hasSize(1);
            assertThat(result.rewardAmount()).isEqualTo(20000L); // 10000 * 2
            assertThat(result.totalShippingCharge()).isEqualTo(3000L);
            assertThat(result.totalBillingAmount()).isEqualTo(23000L);
        }

        // 리워드 2개 선택 → 각 OrderReward 금액 합산 = Order 총금액 검증
        @Test
        void 리워드_2개로_빌링_생성_시_금액_합산이_정확하다() {
            BillingRequestDto reqDto = new BillingRequestDto(savedFunding.getId(), false, List.of(
                    new RewardQuantityDto(savedReward.getId(), 2),
                    new RewardQuantityDto(savedReward2.getId(), 1)
            ));

            BillingResponseDto result = orderService.createBilling(1L, reqDto);

            assertThat(result.rewards()).hasSize(2);
            assertThat(result.rewardAmount()).isEqualTo(40000L); // 10000*2 + 20000*1
            assertThat(result.totalShippingCharge()).isEqualTo(8000L); // 3000 + 5000
            assertThat(result.totalBillingAmount()).isEqualTo(48000L);
        }

        // isReservedOrder=true → RESERVED 상태로 저장
        @Test
        void 예약_주문_빌링은_RESERVED_상태로_저장된다() {
            BillingRequestDto reqDto = new BillingRequestDto(savedFunding.getId(), true, List.of(
                    new RewardQuantityDto(savedReward.getId(), 1)
            ));

            orderService.createBilling(1L, reqDto);

            List<OrderResponseDto> orders = orderService.getAllOrders(1L, PageRequest.of(0, 20));
            assertThat(orders.get(0).orderStatus()).isEqualTo(OrderStatus.RESERVED);
        }

        // isReservedOrder=false → PENDING 상태로 저장
        @Test
        void 일반_주문_빌링은_PENDING_상태로_저장된다() {
            BillingRequestDto reqDto = new BillingRequestDto(savedFunding.getId(), false, List.of(
                    new RewardQuantityDto(savedReward.getId(), 1)
            ));

            orderService.createBilling(1L, reqDto);

            List<OrderResponseDto> orders = orderService.getAllOrders(1L, PageRequest.of(0, 20));
            assertThat(orders.get(0).orderStatus()).isEqualTo(OrderStatus.PENDING);
        }

        // 빌링 생성 시 expiresAt이 설정되는지 검증
        @Test
        void 빌링_생성_시_expiresAt이_설정된다() {
            BillingRequestDto reqDto = new BillingRequestDto(savedFunding.getId(), false, List.of(
                    new RewardQuantityDto(savedReward.getId(), 1)
            ));

            BillingResponseDto result = orderService.createBilling(1L, reqDto);

            assertThat(result.expiresAt()).isNotNull();
            assertThat(result.expiresAt()).isAfter(LocalDateTime.now().plusMinutes(14));
        }
    }

    @Nested
    @DisplayName("주문 생성 통합테스트")
    class CreateOrderIntegrationTest {

        private Long createBillingAndGetOrderId(Long memberId, Long rewardId, int qty) {
            BillingRequestDto reqDto = new BillingRequestDto(savedFunding.getId(), false, List.of(
                    new RewardQuantityDto(rewardId, qty)
            ));
            BillingResponseDto billing = orderService.createBilling(memberId, reqDto);
            return billing.orderId();
        }

        // 정상 빌링 → 결제하기 → 상태 PAYING + 재고 차감 검증
        @Test
        void 주문_생성_시_상태_PAYING으로_변경되고_재고가_차감된다() {
            Long orderId = createBillingAndGetOrderId(1L, savedReward.getId(), 2);

            OrderCreateRequestDto reqDto = new OrderCreateRequestDto(
                    orderId, savedFunding.getId(), true
            );

            OrderCreateResponseDto result = orderService.createOrder(1L, reqDto);

            assertThat(result.orderNo()).isNotNull();
            assertThat(result.totalBillingAmount()).isEqualTo(23000L); // 10000*2 + 3000

            RewardJpaEntity updatedReward = rewardJpaRepository.findById(savedReward.getId()).orElseThrow();
            assertThat(updatedReward.getRemainQty()).isEqualTo(8); // 10 - 2
        }

    }

    @Nested
    @DisplayName("주문 취소 통합테스트")
    class CancelOrderIntegrationTest {

        private Long createAndStartOrder(Long memberId, int qty) {
            BillingRequestDto billingReqDto = new BillingRequestDto(savedFunding.getId(), false, List.of(
                    new RewardQuantityDto(savedReward.getId(), qty)
            ));
            BillingResponseDto billing = orderService.createBilling(memberId, billingReqDto);

            OrderCreateRequestDto createReqDto = new OrderCreateRequestDto(
                    billing.orderId(), savedFunding.getId(), true
            );
            orderService.createOrder(memberId, createReqDto);

            return billing.orderId();
        }

        // PAYING 상태 주문 취소 → CANCELLED + 재고 복구 검증
        @Test
        void 주문_취소_시_CANCELLED_상태로_변경되고_재고가_복구된다() {
            Long orderId = createAndStartOrder(1L, 3);

            OrderCancelResponseDto result = orderService.cancelOrder(1L, orderId, new OrderCancelRequestDto(1L));

            assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(result.cancelledAt()).isNotNull();

            RewardJpaEntity updatedReward = rewardJpaRepository.findById(savedReward.getId()).orElseThrow();
            assertThat(updatedReward.getRemainQty()).isEqualTo(10); // 원복
        }
    }

    @Nested
    @DisplayName("주문 조회 통합테스트")
    class GetOrdersIntegrationTest {

        // 회원 주문 목록 조회 → 본인 주문만 반환
        @Test
        void 본인의_주문_목록만_조회된다() {
            BillingRequestDto reqDto1 = new BillingRequestDto(savedFunding.getId(), false, List.of(
                    new RewardQuantityDto(savedReward.getId(), 1)
            ));
            BillingRequestDto reqDto2 = new BillingRequestDto(savedFunding.getId(), false, List.of(
                    new RewardQuantityDto(savedReward2.getId(), 1)
            ));
            orderService.createBilling(1L, reqDto1);
            orderService.createBilling(1L, reqDto2);
            orderService.createBilling(2L, reqDto1); // 다른 회원

            List<OrderResponseDto> result = orderService.getAllOrders(1L, PageRequest.of(0, 20));

            assertThat(result).hasSize(2);
        }

        // 페이징 동작 검증
        @Test
        void 페이징이_정상_동작한다() {
            for (int i = 0; i < 5; i++) {
                BillingRequestDto reqDto = new BillingRequestDto(savedFunding.getId(), false, List.of(
                        new RewardQuantityDto(savedReward.getId(), 1)
                ));
                orderService.createBilling(1L, reqDto);
            }

            List<OrderResponseDto> page1 = orderService.getAllOrders(1L, PageRequest.of(0, 3));
            List<OrderResponseDto> page2 = orderService.getAllOrders(1L, PageRequest.of(1, 3));

            assertThat(page1).hasSize(3);
            assertThat(page2).hasSize(2);
        }

        // 주문 상세 조회 → 금액/리워드 정보 일치
        @Test
        void 주문_상세_조회_시_금액과_리워드_정보가_일치한다() {
            BillingRequestDto reqDto = new BillingRequestDto(savedFunding.getId(), false, List.of(
                    new RewardQuantityDto(savedReward.getId(), 2),
                    new RewardQuantityDto(savedReward2.getId(), 1)
            ));
            BillingResponseDto billing = orderService.createBilling(1L, reqDto);

            OrderDetailResponseDto result = orderService.getOrderDetail(1L, billing.orderId());

            assertThat(result.orderNo()).isNotNull();
            assertThat(result.rewards()).hasSize(2);
            assertThat(result.rewardAmount()).isEqualTo(40000L);
            assertThat(result.totalShippingCharge()).isEqualTo(8000L);
            assertThat(result.totalBillingAmount()).isEqualTo(48000L);
        }
    }

    @Nested
    @DisplayName("동시성 제어 통합테스트")
    class ConcurrencyIntegrationTest {

        private RewardJpaEntity createRewardWithStock(int stock) {
            return rewardJpaRepository.save(new RewardJpaEntity(
                    null, savedFunding, "동시성테스트리워드", "설명", 100, stock,
                    null, 10000L, LocalDateTime.now().plusDays(60), 3000L
            ));
        }

        // 동일 유저가 동일 주문에 10번 동시 결제 요청 → Lock(NOWAIT)에 의해 1번만 성공 (더블클릭 시나리오)
        @Test
        void 동일_유저가_동일_주문에_동시_요청_시_1건만_성공한다() throws InterruptedException {
            BillingRequestDto billingReqDto = new BillingRequestDto(savedFunding.getId(), false, List.of(
                    new RewardQuantityDto(savedReward.getId(), 1)
            ));
            BillingResponseDto billing = orderService.createBilling(1L, billingReqDto);

            OrderCreateRequestDto reqDto = new OrderCreateRequestDto(
                    billing.orderId(), savedFunding.getId(), true
            );

            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        orderService.createOrder(1L, reqDto);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            assertThat(successCount.get()).isEqualTo(1);
            assertThat(failCount.get()).isEqualTo(threadCount - 1);
        }

        // 재고 1개인 리워드에 5명 동시 주문 → CAS(remainQty >= qty)에 의해 1명만 성공
        @Test
        void 재고_1개에_5명_동시_주문_시_CAS에_의해_1명만_성공한다() throws InterruptedException {
            RewardJpaEntity rewardStock1 = createRewardWithStock(1);

            int threadCount = 5;
            List<Long> orderIds = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                long memberId = 100L + i;
                BillingRequestDto billingReqDto = new BillingRequestDto(savedFunding.getId(), false, List.of(
                        new RewardQuantityDto(rewardStock1.getId(), 1)
                ));
                BillingResponseDto billing = orderService.createBilling(memberId, billingReqDto);
                orderIds.add(billing.orderId());
            }

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                long memberId = 100L + i;
                Long orderId = orderIds.get(i);
                executor.submit(() -> {
                    try {
                        OrderCreateRequestDto reqDto = new OrderCreateRequestDto(
                                orderId, savedFunding.getId(), true
                        );
                        orderService.createOrder(memberId, reqDto);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            assertThat(successCount.get()).isEqualTo(1);
            assertThat(failCount.get()).isEqualTo(4);

            RewardJpaEntity updatedReward = rewardJpaRepository.findById(rewardStock1.getId()).orElseThrow();
            assertThat(updatedReward.getRemainQty()).isEqualTo(0);
        }

        // 재고 N개, 동시 요청 M명(각 1개씩) → 성공 수 = min(N, M), 최종 재고 = N - 성공 수
        @ParameterizedTest(name = "재고 {0}개, 동시 {1}명 → {2}명 성공")
        @CsvSource({
                "3, 5, 3",
                "5, 5, 5",
                "2, 10, 2",
                "7, 3, 3"
        })
        void 재고_N개에_M명_동시_주문_시_정합성이_유지된다(int stock, int threadCount, int expectedSuccess) throws InterruptedException {
            RewardJpaEntity rewardWithStock = createRewardWithStock(stock);

            List<Long> orderIds = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                long memberId = 200L + i;
                BillingRequestDto billingReqDto = new BillingRequestDto(savedFunding.getId(), false, List.of(
                        new RewardQuantityDto(rewardWithStock.getId(), 1)
                ));
                BillingResponseDto billing = orderService.createBilling(memberId, billingReqDto);
                orderIds.add(billing.orderId());
            }

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                long memberId = 200L + i;
                Long orderId = orderIds.get(i);
                executor.submit(() -> {
                    try {
                        OrderCreateRequestDto reqDto = new OrderCreateRequestDto(
                                orderId, savedFunding.getId(), true
                        );
                        orderService.createOrder(memberId, reqDto);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        // 재고 부족 or Lock 실패
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            assertThat(successCount.get()).isEqualTo(expectedSuccess);

            RewardJpaEntity updatedReward = rewardJpaRepository.findById(rewardWithStock.getId()).orElseThrow();
            assertThat(updatedReward.getRemainQty()).isEqualTo(stock - expectedSuccess);
        }

        // 주문 취소와 동일 주문 결제 요청 동시 진행 → Lock에 의해 하나만 성공
        @Test
        void 취소와_결제_동시_요청_시_하나만_성공한다() throws InterruptedException {
            BillingRequestDto billingReqDto = new BillingRequestDto(savedFunding.getId(), false, List.of(
                    new RewardQuantityDto(savedReward.getId(), 2)
            ));
            BillingResponseDto billing = orderService.createBilling(1L, billingReqDto);

            OrderCreateRequestDto createReqDto = new OrderCreateRequestDto(
                    billing.orderId(), savedFunding.getId(), true
            );
            orderService.createOrder(1L, createReqDto);

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch latch = new CountDownLatch(2);
            AtomicInteger cancelSuccess = new AtomicInteger(0);
            AtomicInteger retrySuccess = new AtomicInteger(0);

            // 취소 시도
            executor.submit(() -> {
                try {
                    orderService.cancelOrder(1L, billing.orderId(), new OrderCancelRequestDto(1L));
                    cancelSuccess.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });

            // 동시에 다시 결제 시도 (이미 PAYING이므로 상태 전이 실패)
            executor.submit(() -> {
                try {
                    orderService.createOrder(1L, createReqDto);
                    retrySuccess.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });

            latch.await();
            executor.shutdown();

            // 취소만 성공하거나, Lock에 의해 하나만 처리됨
            assertThat(cancelSuccess.get() + retrySuccess.get()).isEqualTo(1);
        }

        // 빌링 5건 생성(재고 미차감) → 이후 주문 생성 시 CAS에 의해 재고 한도 내에서만 성공
        @ParameterizedTest(name = "빌링 {1}건 생성, 재고 {0}개 → 주문 {2}건만 성공")
        @CsvSource({
                "3, 5, 3",
                "1, 5, 1",
                "5, 3, 3"
        })
        void 빌링은_모두_성공하지만_주문은_재고_한도_내에서만_성공한다(int stock, int billingCount, int expectedSuccess) throws InterruptedException {
            RewardJpaEntity rewardWithStock = createRewardWithStock(stock);

            // 빌링 생성 (재고 미차감이므로 모두 성공)
            List<Long> orderIds = new ArrayList<>();
            for (int i = 0; i < billingCount; i++) {
                long memberId = 300L + i;
                BillingRequestDto billingReqDto = new BillingRequestDto(savedFunding.getId(), false, List.of(
                        new RewardQuantityDto(rewardWithStock.getId(), 1)
                ));
                BillingResponseDto billing = orderService.createBilling(memberId, billingReqDto);
                orderIds.add(billing.orderId());
            }

            // 동시 주문 생성
            ExecutorService executor = Executors.newFixedThreadPool(billingCount);
            CountDownLatch latch = new CountDownLatch(billingCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < billingCount; i++) {
                long memberId = 300L + i;
                Long orderId = orderIds.get(i);
                executor.submit(() -> {
                    try {
                        OrderCreateRequestDto reqDto = new OrderCreateRequestDto(
                                orderId, savedFunding.getId(), true
                        );
                        orderService.createOrder(memberId, reqDto);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        // 재고 부족
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            assertThat(successCount.get()).isEqualTo(expectedSuccess);

            RewardJpaEntity updatedReward = rewardJpaRepository.findById(rewardWithStock.getId()).orElseThrow();
            assertThat(updatedReward.getRemainQty()).isEqualTo(stock - expectedSuccess);
        }
    }

    @Nested
    @DisplayName("전체 플로우 통합테스트")
    class FullFlowIntegrationTest {

        // 빌링 생성 → 주문 생성 → 취소 → 재고 복구까지 전체 플로우
        @Test
        void 빌링_생성부터_취소까지_전체_플로우가_정상_동작한다() {
            // 1. 빌링 생성 (PENDING)
            BillingRequestDto billingReqDto = new BillingRequestDto(savedFunding.getId(), false, List.of(
                    new RewardQuantityDto(savedReward.getId(), 2)
            ));
            BillingResponseDto billingResult = orderService.createBilling(1L, billingReqDto);
            assertThat(billingResult.rewardAmount()).isEqualTo(20000L);

            // 2. 주문 생성 (PAYING), 재고 차감
            OrderCreateRequestDto createReqDto = new OrderCreateRequestDto(
                    billingResult.orderId(), savedFunding.getId(), true
            );
            OrderCreateResponseDto createResult = orderService.createOrder(1L, createReqDto);
            assertThat(createResult.orderNo()).isNotNull();

            RewardJpaEntity afterCreate = rewardJpaRepository.findById(savedReward.getId()).orElseThrow();
            assertThat(afterCreate.getRemainQty()).isEqualTo(8); // 10 - 2

            // 3. 주문 취소 (CANCELLED), 재고 복구
            OrderCancelResponseDto cancelResult = orderService.cancelOrder(1L, billingResult.orderId(), new OrderCancelRequestDto(1L));
            assertThat(cancelResult.status()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(cancelResult.cancelledAt()).isNotNull();

            RewardJpaEntity afterCancel = rewardJpaRepository.findById(savedReward.getId()).orElseThrow();
            assertThat(afterCancel.getRemainQty()).isEqualTo(10); // 원복
        }

        // 취소된 주문에 다시 결제 시작 → 상태 전이 불가
        @Test
        void 취소된_주문은_다시_결제를_시작할_수_없다() {
            BillingRequestDto billingReqDto = new BillingRequestDto(savedFunding.getId(), false, List.of(
                    new RewardQuantityDto(savedReward.getId(), 1)
            ));
            BillingResponseDto billing = orderService.createBilling(1L, billingReqDto);

            OrderCreateRequestDto createReqDto = new OrderCreateRequestDto(
                    billing.orderId(), savedFunding.getId(), true
            );
            orderService.createOrder(1L, createReqDto);
            orderService.cancelOrder(1L, billing.orderId(), new OrderCancelRequestDto(1L));

            // 취소 후 다시 결제 시도
            assertThrows(Exception.class, () ->
                    orderService.createOrder(1L, createReqDto)
            );
        }
    }

    @Nested
    @DisplayName("processStatusUpdate 통합테스트")
    class ProcessStatusUpdateIntegrationTest {

        // PAYING 상태 주문 → PAID로 상태 변경 → DB 반영 확인
        @Transactional
        @Test
        void PAYING_주문을_PAID로_변경하면_DB에_반영된다() {
            Order order = createOrderWithStatus(OrderStatus.PAYING);

            orderService.processStatusUpdate(order.getId(), OrderStatus.PAID);

            Order updated = orderRepository.findByIdForUpdate(order.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.PAID);
        }

        // 전이 불가능한 상태(REFUNDED → PAID) → 상태 유지
        @Transactional
        @Test
        void 전이_불가능하면_상태가_변경되지_않는다() {
            Order order = createOrderWithStatus(OrderStatus.REFUNDED);

            orderService.processStatusUpdate(order.getId(), OrderStatus.PAID);

            Order updated = orderRepository.findByIdForUpdate(order.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        }

        // 존재하지 않는 orderId → 예외 없이 조용히 처리
        @Test
        void 존재하지_않는_주문이면_아무_동작도_하지_않는다() {
            orderService.processStatusUpdate(9999L, OrderStatus.PAID);
        }
    }

    @Nested
    @DisplayName("processCancelledUpdate 통합테스트")
    class ProcessCancelledUpdateIntegrationTest {

        // PAYING 주문 → CANCELLED + cancelReason 저장 확인
        @Transactional
        @Test
        void PAYING_주문을_CANCELLED로_변경하고_cancelReason이_저장된다() {
            Order order = createOrderWithStatus(OrderStatus.PAYING);

            orderService.processCancelledUpdate(order.getId(), "PAYMENT_CANCELLED");

            Order updated = orderRepository.findByIdForUpdate(order.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(updated.getCancelReason()).isEqualTo("PAYMENT_CANCELLED");
        }

        // 취소 시 reward 재고 복구 확인
        @Transactional
        @Test
        void 취소_시_재고가_복구된다() {
            int initialRemainQty = savedReward.getRemainQty();
            Order order = createOrderWithStatus(OrderStatus.PAYING);

            orderService.processCancelledUpdate(order.getId(), "PAYMENT_CANCELLED");

            entityManager.flush();
            entityManager.clear();

            RewardJpaEntity updatedReward = rewardJpaRepository.findById(savedReward.getId()).orElseThrow();
            assertThat(updatedReward.getRemainQty()).isEqualTo(initialRemainQty + 1);
        }
    }

    @Nested
    @DisplayName("processReservedFundingConfirmed 통합테스트")
    class ProcessPayingAndPublishSettlementIntegrationTest {

        // RESERVED 주문 → PAYING 상태 변경 → DB 반영 확인
        @Transactional
        @Test
        void RESERVED_주문을_PAYING으로_변경하면_DB에_반영된다() {
            Order order = createOrderWithStatus(OrderStatus.RESERVED);

            orderService.processReservedFundingConfirmed(order.getId());

            Order updated = orderRepository.findByIdForUpdate(order.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.PAYING);
        }
    }

    @Nested
    @DisplayName("processFundingFailedRefund 통합테스트")
    class processFundingFailedRefundIntegrationTest {

        // PAID 주문 → CANCELLED + FUNDING_FAILED reason 저장 확인
        @Transactional
        @Test
        void PAID_주문을_CANCELLED로_변경하고_FUNDING_FAILED가_저장된다() {
            Order order = createOrderWithStatus(OrderStatus.PAID);

            orderService.processFundingFailedRefund(order.getId());

            Order updated = orderRepository.findByIdForUpdate(order.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(updated.getCancelReason()).isEqualTo("FUNDING_FAILED");
        }

        // 취소 시 reward 재고 복구 확인
        @Transactional
        @Test
        void 취소_시_재고가_복구된다() {
            int initialRemainQty = savedReward.getRemainQty();
            Order order = createOrderWithStatus(OrderStatus.PAID);

            orderService.processFundingFailedRefund(order.getId());

            entityManager.flush();
            entityManager.clear();

            RewardJpaEntity updatedReward = rewardJpaRepository.findById(savedReward.getId()).orElseThrow();
            assertThat(updatedReward.getRemainQty()).isEqualTo(initialRemainQty + 1);
        }
    }
}
