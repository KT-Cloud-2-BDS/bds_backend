package com.bds.chat.presentation.chatRoom;

import com.bds.chat.application.chatRoom.dto.*;
import com.bds.chat.application.chatRoom.service.ChatRoomService;
import com.bds.chat.application.member.dto.InquiryMemberLeaveResponseDto;
import com.bds.chat.application.member.service.InquiryRoomMemberService;
import com.bds.chat.presentation.GlobalExceptionHandler;
import com.bds.common.resolver.LoginUserArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChatRoomControllerUnitTest {

    @Mock ChatRoomService chatRoomService;
    @Mock InquiryRoomMemberService inquiryRoomMemberService;

    @InjectMocks ChatRoomController controller;

    private MockMvc mockMvc;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);


    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new LoginUserArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private ChatRoomResponseDto dummyChatRoomResponse(String type) {
        return new ChatRoomResponseDto(10L, type, 1L, List.of(5L), 2L, NOW, "ACTIVE");
    }

    private InquiryRoomListResponseDto dummyRoomList() {
        return new InquiryRoomListResponseDto(List.of(), null, false, 0);
    }

    private InquiryChatRoomDetailResponseDto dummyInquiryDetail() {
        MembershipStatusDto membership = new MembershipStatusDto("ACTIVE", null, NOW);
        return new InquiryChatRoomDetailResponseDto(10L, "INQUIRY", 1L, List.of(5L), 2L, NOW, "ACTIVE", membership, null);
    }

    private ChatRoomDeleteResponseDto dummyDeleteResponse() {
        return new ChatRoomDeleteResponseDto(10L, true, NOW);
    }

    private InquiryMemberLeaveResponseDto dummyLeaveResponse() {
        return new InquiryMemberLeaveResponseDto(10L, 5L, true, NOW);
    }

    @Nested
    @DisplayName("문의방 생성")
    class CreateInquiryRoomTest {

        @Test
        void 문의방_생성이_201을_반환한다() throws Exception {
            given(chatRoomService.createInquiryRoom(anyLong(), anyLong()))
                    .willReturn(dummyChatRoomResponse("INQUIRY"));

            mockMvc.perform(post("/api/chat/Inquiries")
                            .header("X-User-Id", "5")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"productId\": 1}"))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("내 문의방 목록 조회")
    class GetMyInquiryRoomsTest {

        @Test
        void 문의방_목록_조회가_200을_반환한다() throws Exception {
            given(chatRoomService.getMyInquiryRooms(anyLong(), any(), anyInt()))
                    .willReturn(dummyRoomList());

            mockMvc.perform(get("/api/chat/Inquiries")
                            .header("X-User-Id", "5"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("문의방 상세 조회")
    class GetInquiryRoomTest {

        @Test
        void 문의방_상세_조회가_200을_반환한다() throws Exception {
            given(chatRoomService.getInquiryChatRoomById(anyLong(), anyLong()))
                    .willReturn(dummyInquiryDetail());

            mockMvc.perform(get("/api/chat/Inquiries/10")
                            .header("X-User-Id", "5"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("채팅방 삭제")
    class CloseRoomTest {

        @Test
        void 채팅방_삭제가_200을_반환한다() throws Exception {
            given(chatRoomService.delete(anyLong(), anyLong()))
                    .willReturn(dummyDeleteResponse());

            mockMvc.perform(delete("/api/chat/rooms/10/close")
                            .header("X-User-Id", "5"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("문의방 나가기")
    class LeaveInquiryRoomTest {

        @Test
        void 문의방_나가기가_200을_반환한다() throws Exception {
            given(inquiryRoomMemberService.leave(anyLong(), anyLong()))
                    .willReturn(dummyLeaveResponse());

            mockMvc.perform(delete("/api/chat/Inquiries/10/members/me")
                            .header("X-User-Id", "5"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("펀딩방 조회")
    class GetFundingRoomTest {

        @Test
        void 펀딩방_조회가_200을_반환한다() throws Exception {
            given(chatRoomService.getFundingChatRoomById(anyLong()))
                    .willReturn(dummyChatRoomResponse("FUNDING"));

            mockMvc.perform(get("/api/chat/fundings/1"))
                    .andExpect(status().isOk());
        }
    }
}
