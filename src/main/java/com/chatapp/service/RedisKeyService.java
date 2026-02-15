package com.chatapp.service;

import org.springframework.stereotype.Component;

@Component
public class RedisKeyService {

    private static final String ROOMS_SET = "chatapp:rooms";

    public String roomsSet() {
        return ROOMS_SET;
    }

    public String roomMeta(String roomId) {
        return "chatapp:rooms:" + roomId + ":meta";
    }

    public String roomParticipants(String roomId) {
        return "chatapp:rooms:" + roomId + ":participants";
    }

    public String roomMessages(String roomId) {
        return "chatapp:rooms:" + roomId + ":messages";
    }

    public String roomChannel(String roomId) {
        return "chatapp:rooms:" + roomId + ":channel";
    }
}
