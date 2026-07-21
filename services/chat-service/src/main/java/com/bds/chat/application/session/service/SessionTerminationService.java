package com.bds.chat.application.session.service;

public interface SessionTerminationService {
    void terminateUser(String userId, String reason);
    void terminateSession(String sessionId, String reason);
}
