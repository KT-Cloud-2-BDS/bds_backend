package com.bds.chat.common;

public class DuplicateClientIdException extends RuntimeException {

    private final String clientId;

    public DuplicateClientIdException(String clientId) {
        super("Duplicate clientId: " + clientId);
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }
}
