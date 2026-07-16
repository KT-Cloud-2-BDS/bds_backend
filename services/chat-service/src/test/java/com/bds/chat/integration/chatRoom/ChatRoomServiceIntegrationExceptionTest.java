package com.bds.chat.integration.chatRoom;

import com.bds.chat.application.chatRoom.dto.ChatRoomResponseDto;
import com.bds.chat.application.chatRoom.dto.FundingRoomCreateRequestDto;
import com.bds.chat.application.chatRoom.service.ChatRoomService;
import com.bds.chat.common.BusinessException;
import com.bds.chat.common.ErrorCode;
import com.bds.chat.config.TestContainersConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local")
@Import(TestContainersConfig.class)
@Transactional
@DisplayName("채팅방 서비스 통합 예외 테스트")
class ChatRoomServiceIntegrationExceptionTest {

    @Autowired private ChatRoomService chatRoomService;

    private static final Long SELLER_ID = 2L;
    private static final Long BUYER_ID = 7L;
    private static final Long PRODUCT_ID = 5L;
    private static final Long UNKNOWN_ROOM_ID = 999999L;

    @Nested
    @DisplayName("펀딩 채팅방 생성 예외")
    class CreateFundingRoomExceptionTest {

        // 동일 상품 중복 생성 → CONFLICT
        @Test
        @DisplayName("이미 존재하는 상품의 펀딩 채팅방을 생성하면 CONFLICT 예외가 발생한다")
        void 이미_존재하는_상품의_펀딩_채팅방을_생성하면_CONFLICT_예외() {
            chatRoomService.createFundingRoom(PRODUCT_ID, new FundingRoomCreateRequestDto(SELLER_ID));

            assertThatThrownBy(() ->
                    chatRoomService.createFundingRoom(PRODUCT_ID, new FundingRoomCreateRequestDto(SELLER_ID))
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        }
    }

    @Nested
    @DisplayName("문의 채팅방 생성 예외")
    class CreateInquiryRoomExceptionTest {

        // 펀딩방 없이 문의방 생성 → 판매자 조회 실패 (NOT_FOUND)
        @Test
        @DisplayName("펀딩 채팅방이 없는 상품으로 문의 채팅방 생성 시 NOT_FOUND 예외가 발생한다")
        void 펀딩방_없으면_문의방_생성_시_NOT_FOUND_예외() {
            assertThatThrownBy(() ->
                    chatRoomService.createInquiryRoom(PRODUCT_ID, BUYER_ID)
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("채팅방 삭제 예외")
    class DeleteExceptionTest {

        // 존재하지 않는 방 삭제 → NOT_FOUND
        @Test
        @DisplayName("존재하지 않는 채팅방을 삭제하면 NOT_FOUND 예외가 발생한다")
        void 존재하지_않는_채팅방을_삭제하면_NOT_FOUND_예외() {
            assertThatThrownBy(() ->
                    chatRoomService.delete(UNKNOWN_ROOM_ID, SELLER_ID)
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        // 방장이 아닌 사용자 삭제 시도 → FORBIDDEN
        @Test
        @DisplayName("방장이 아닌 사용자가 채팅방을 삭제하면 FORBIDDEN 예외가 발생한다")
        void 방장이_아닌_사용자가_채팅방을_삭제하면_FORBIDDEN_예외() {
            ChatRoomResponseDto created = chatRoomService.createFundingRoom(PRODUCT_ID, new FundingRoomCreateRequestDto(SELLER_ID));

            assertThatThrownBy(() ->
                    chatRoomService.delete(created.roomId(), 99L)
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }
    }

    @Nested
    @DisplayName("펀딩 채팅방 단건 조회 예외")
    class GetFundingChatRoomByIdExceptionTest {

        // 존재하지 않는 방 조회 → NOT_FOUND
        @Test
        @DisplayName("존재하지 않는 펀딩 채팅방을 조회하면 NOT_FOUND 예외가 발생한다")
        void 존재하지_않는_방을_조회하면_NOT_FOUND_예외() {
            assertThatThrownBy(() ->
                    chatRoomService.getFundingChatRoomById(UNKNOWN_ROOM_ID)
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        // INQUIRY 방을 펀딩방 API로 조회 → NOT_FOUND (타입 불일치)
        @Test
        @DisplayName("INQUIRY 채팅방을 펀딩방 조회 API로 조회하면 NOT_FOUND 예외가 발생한다")
        void INQUIRY_방을_펀딩방_API로_조회하면_NOT_FOUND_예외() {
            chatRoomService.createFundingRoom(PRODUCT_ID, new FundingRoomCreateRequestDto(SELLER_ID));
            ChatRoomResponseDto inquiryRoom = chatRoomService.createInquiryRoom(PRODUCT_ID, BUYER_ID);

            assertThatThrownBy(() ->
                    chatRoomService.getFundingChatRoomById(inquiryRoom.roomId())
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("입력값 유효성 예외")
    class InvalidInputTest {

        // 0인 productId → ProductId.of(0L) 생성 시 INVALID_INPUT
        @Test
        @DisplayName("0인 productId로 펀딩방 생성 시 INVALID_INPUT 예외가 발생한다")
        void 잘못된_productId로_펀딩방_생성_시_INVALID_INPUT_예외() {
            assertThatThrownBy(() ->
                    chatRoomService.createFundingRoom(0L, new FundingRoomCreateRequestDto(SELLER_ID))
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        }

        // 음수 creatorId → MemberId.of(-1L) 생성 시 INVALID_INPUT
        @Test
        @DisplayName("음수 creatorId로 펀딩방 생성 시 INVALID_INPUT 예외가 발생한다")
        void 잘못된_creatorId로_펀딩방_생성_시_INVALID_INPUT_예외() {
            assertThatThrownBy(() ->
                    chatRoomService.createFundingRoom(PRODUCT_ID, new FundingRoomCreateRequestDto(-1L))
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        }
    }

    @Nested
    @DisplayName("문의 채팅방 상세 조회 예외")
    class GetInquiryChatRoomByIdExceptionTest {

        // memberId 없이 문의방 조회 → FORBIDDEN
        @Test
        @DisplayName("memberId 없이 문의방을 조회하면 FORBIDDEN 예외가 발생한다")
        void memberId_없이_문의방_조회하면_FORBIDDEN_예외() {
            assertThatThrownBy(() ->
                    chatRoomService.getInquiryChatRoomById(UNKNOWN_ROOM_ID, null)
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }

        // 멤버가 아닌 사용자가 문의방 조회 → FORBIDDEN
        @Test
        @DisplayName("멤버가 아닌 사용자가 문의방을 조회하면 FORBIDDEN 예외가 발생한다")
        void 멤버가_아닌_사용자가_문의방_조회하면_FORBIDDEN_예외() {
            chatRoomService.createFundingRoom(PRODUCT_ID, new FundingRoomCreateRequestDto(SELLER_ID));
            ChatRoomResponseDto inquiry = chatRoomService.createInquiryRoom(PRODUCT_ID, BUYER_ID);

            assertThatThrownBy(() ->
                    chatRoomService.getInquiryChatRoomById(inquiry.roomId(), 99L)
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }
    }
}
