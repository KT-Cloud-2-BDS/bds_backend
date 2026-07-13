package com.bds.chat.presentation.blackList;

import com.bds.chat.application.blackList.dto.BlackListCreateRequestDto;
import com.bds.chat.application.blackList.dto.BlackListReponseDto;
import com.bds.chat.application.blackList.service.BlackListService;
import com.bds.common.annotation.LoginUser;
import com.bds.common.dto.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat/fundings")
@RequiredArgsConstructor
public class BlackListController {

    private final BlackListService blackListService;

    // 공개 채팅방 사용자 BAN
    @PostMapping("/{roomId}/ban")
    public ResponseEntity<BlackListReponseDto> ban(
            @PathVariable Long roomId,
            @RequestBody BlackListCreateRequestDto request,
            @LoginUser CurrentUser currentUser
    ) {
        BlackListReponseDto response = blackListService.create(roomId, currentUser.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 공개 채팅방 사용자 BAN 해제
    @DeleteMapping("/{roomId}/ban/{targetId}")
    public ResponseEntity<BlackListReponseDto> releaseBan(
            @PathVariable Long roomId,
            @PathVariable Long targetId,
            @LoginUser CurrentUser currentUser
    ) {
        BlackListReponseDto response = blackListService.delete(roomId, currentUser.id(), targetId);
        return ResponseEntity.ok(response);
    }
}
