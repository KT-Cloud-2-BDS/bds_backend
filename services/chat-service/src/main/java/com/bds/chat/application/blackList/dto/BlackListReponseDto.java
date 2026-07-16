package com.bds.chat.application.blackList.dto;

public record BlackListReponseDto(
        Long roomId,
        Long bannedUserId,
        String status
) {

}
