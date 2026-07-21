package com.bds.chat.integration.message;

import com.bds.chat.application.blackList.dto.BlackListCreateRequestDto;
import com.bds.chat.application.blackList.service.BlackListService;
import com.bds.chat.application.chatRoom.dto.ChatRoomResponseDto;
import com.bds.chat.application.chatRoom.dto.FundingRoomCreateRequestDto;
import com.bds.chat.application.chatRoom.service.ChatRoomService;
import com.bds.chat.application.message.dto.MessageResponseDto;
import com.bds.chat.application.message.dto.MessageSendRequestDto;
import com.bds.chat.application.message.service.MessageService;
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
@DisplayName("메시지 서비스 통합 예외 테스트")
class MessageServiceIntegrationExceptionTest {

    @Autowired private MessageService messageService;
    @Autowired private ChatRoomService chatRoomService;
    @Autowired private BlackListService blackListService;

    private static final Long SELLER_ID = 2L;
    private static final Long BUYER_ID = 7L;
    private static final Long PRODUCT_ID = 10L;
    private static final Long UNKNOWN_ROOM_ID = 999999L;
    private static final Long UNKNOWN_MESSAGE_ID = 999999L;

    private Long setupFundingRoom() {
        return chatRoomService.createFundingRoom(PRODUCT_ID, new FundingRoomCreateRequestDto(SELLER_ID)).roomId();
    }

    private Long setupInquiryRoom() {
        setupFundingRoom();
        return chatRoomService.createInquiryRoom(PRODUCT_ID, BUYER_ID).roomId();
    }

    @Nested
    @DisplayName("메시지 생성 예외")
    class CreateExceptionTest {

        // 존재하지 않는 방에 전송 → NOT_FOUND
        @Test
        @DisplayName("존재하지 않는 채팅방에 메시지를 전송하면 NOT_FOUND 예외가 발생한다")
        void 존재하지_않는_방에_메시지를_전송하면_NOT_FOUND_예외() {
            assertThatThrownBy(() ->
                    messageService.create(new MessageSendRequestDto(UNKNOWN_ROOM_ID, "hello", "TEXT", null), BUYER_ID)
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        // 블랙리스트에 등록된 사용자가 펀딩방에 전송 → FORBIDDEN
        @Test
        @DisplayName("블랙리스트에 등록된 사용자가 메시지를 전송하면 FORBIDDEN 예외가 발생한다")
        void 블랙리스트_사용자가_메시지를_전송하면_FORBIDDEN_예외() {
            Long roomId = setupFundingRoom();
            blackListService.create(roomId, SELLER_ID, new BlackListCreateRequestDto(BUYER_ID, "spam"));

            assertThatThrownBy(() ->
                    messageService.create(new MessageSendRequestDto(roomId, "hello", "TEXT", null), BUYER_ID)
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }

        // 문의방 비멤버가 전송 → FORBIDDEN
        @Test
        @DisplayName("문의 채팅방 비멤버가 메시지를 전송하면 FORBIDDEN 예외가 발생한다")
        void 문의방_비멤버가_메시지를_전송하면_FORBIDDEN_예외() {
            Long inquiryRoomId = setupInquiryRoom();

            assertThatThrownBy(() ->
                    messageService.create(new MessageSendRequestDto(inquiryRoomId, "hello", "TEXT", null), 99L)
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }
    }

    @Nested
    @DisplayName("메시지 삭제 예외")
    class DeleteExceptionTest {

        // 존재하지 않는 메시지 삭제 → NOT_FOUND
        @Test
        @DisplayName("존재하지 않는 메시지를 삭제하면 NOT_FOUND 예외가 발생한다")
        void 존재하지_않는_메시지를_삭제하면_NOT_FOUND_예외() {
            assertThatThrownBy(() ->
                    messageService.delete(UNKNOWN_MESSAGE_ID, BUYER_ID)
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        // 이미 삭제된 메시지 재삭제 → NOT_FOUND
        @Test
        @DisplayName("이미 삭제된 메시지를 다시 삭제하면 NOT_FOUND 예외가 발생한다")
        void 이미_삭제된_메시지를_다시_삭제하면_NOT_FOUND_예외() {
            Long roomId = setupFundingRoom();
            MessageResponseDto created = messageService.create(
                    new MessageSendRequestDto(roomId, "삭제 대상", "TEXT", null), BUYER_ID);
            messageService.delete(created.messageId(), BUYER_ID);

            assertThatThrownBy(() ->
                    messageService.delete(created.messageId(), BUYER_ID)
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        // 발신자도 방장도 아닌 사용자가 삭제 → FORBIDDEN
        @Test
        @DisplayName("발신자도 방장도 아닌 사용자가 메시지를 삭제하면 FORBIDDEN 예외가 발생한다")
        void 발신자도_방장도_아닌_사용자가_메시지를_삭제하면_FORBIDDEN_예외() {
            Long roomId = setupFundingRoom();
            MessageResponseDto created = messageService.create(
                    new MessageSendRequestDto(roomId, "내 메시지", "TEXT", null), BUYER_ID);

            assertThatThrownBy(() ->
                    messageService.delete(created.messageId(), 99L)
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }
    }

    @Nested
    @DisplayName("펀딩방 메시지 조회 예외")
    class GetFundingMessagesExceptionTest {

        // INQUIRY 방을 펀딩방 API로 조회 → NOT_FOUND (타입 불일치)
        @Test
        @DisplayName("INQUIRY 채팅방을 펀딩방 메시지 조회 API로 조회하면 NOT_FOUND 예외가 발생한다")
        void INQUIRY_방을_펀딩방_API로_조회하면_NOT_FOUND_예외() {
            Long inquiryRoomId = setupInquiryRoom();

            assertThatThrownBy(() ->
                    messageService.getFundingMessages(inquiryRoomId, null)
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("문의방 메시지 조회 예외")
    class GetInquiryMessagesExceptionTest {

        // INQUIRY 방에서 memberId 없이 조회 → NOT_FOUND (XOR 조건 위반)
        @Test
        @DisplayName("INQUIRY 채팅방을 memberId 없이 조회하면 FORBIDDEN 예외가 발생한다")
        void INQUIRY_방을_memberId_없이_조회하면_NOT_FOUND_예외() {
            Long inquiryRoomId = setupInquiryRoom();

            assertThatThrownBy(() ->
                    messageService.getInquiryMessages(inquiryRoomId, null, null)
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }

        // 문의방 비멤버가 조회 → FORBIDDEN
        @Test
        @DisplayName("문의 채팅방 비멤버가 메시지를 조회하면 FORBIDDEN 예외가 발생한다")
        void 문의방_비멤버가_메시지를_조회하면_FORBIDDEN_예외() {
            Long inquiryRoomId = setupInquiryRoom();

            assertThatThrownBy(() ->
                    messageService.getInquiryMessages(inquiryRoomId, 99L, null)
            ).isInstanceOf(BusinessException.class)
             .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }
    }
}
