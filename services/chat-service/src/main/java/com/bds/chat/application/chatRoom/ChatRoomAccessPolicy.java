package com.bds.chat.application.chatRoom;

import java.util.Optional;

public interface ChatRoomAccessPolicy {
    boolean canSubscribe(Long roomId, Optional<String> userId);
}
