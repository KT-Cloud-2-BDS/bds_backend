package com.bds.chat.domain.message;

public record LatestWithUnread(ChatMessage latest, long unreadCount) {}
