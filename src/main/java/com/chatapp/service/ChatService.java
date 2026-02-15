package com.chatapp.service;

import com.chatapp.dto.ChatMessage;
import com.chatapp.exception.DuplicateRoomException;
import com.chatapp.exception.RoomNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ChatService {

    private final StringRedisTemplate redisTemplate;
    private final RedisKeyService keys;
    private final ObjectMapper objectMapper;

    public ChatService(StringRedisTemplate redisTemplate, RedisKeyService keys, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.keys = keys;
        this.objectMapper = objectMapper;
    }

    public String createRoom(String roomId) {
        String roomKey = normalizeRoomId(roomId);
        Boolean exists = redisTemplate.opsForSet().isMember(keys.roomsSet(), roomKey);
        if (Boolean.TRUE.equals(exists)) {
            throw new DuplicateRoomException("Chat room '" + roomKey + "' already exists.");
        }

        redisTemplate.opsForSet().add(keys.roomsSet(), roomKey);
        redisTemplate.opsForHash().put(keys.roomMeta(roomKey), "name", roomKey);
        redisTemplate.opsForHash().put(keys.roomMeta(roomKey), "createdAt", Instant.now().toString());
        return roomKey;
    }

    public void joinRoom(String roomId, String participant) {
        String normalizedRoom = normalizeRoomId(roomId);
        assertRoomExists(normalizedRoom);
        redisTemplate.opsForSet().add(keys.roomParticipants(normalizedRoom), participant.trim());
    }

    public void sendMessage(String roomId, String participant, String message) {
        String normalizedRoom = normalizeRoomId(roomId);
        assertRoomExists(normalizedRoom);
        ChatMessage chatMessage = new ChatMessage(participant.trim(), message.trim(), Instant.now().toString());

        try {
            String payload = objectMapper.writeValueAsString(chatMessage);
            redisTemplate.opsForList().rightPush(keys.roomMessages(normalizedRoom), payload);
            redisTemplate.convertAndSend(keys.roomChannel(normalizedRoom), payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize message", exception);
        }
    }

    public List<ChatMessage> getMessages(String roomId, int limit) {
        String normalizedRoom = normalizeRoomId(roomId);
        assertRoomExists(normalizedRoom);
        int boundedLimit = Math.max(limit, 1);
        List<String> rawMessages = redisTemplate.opsForList()
                .range(keys.roomMessages(normalizedRoom), -boundedLimit, -1);
        if (rawMessages == null || rawMessages.isEmpty()) {
            return Collections.emptyList();
        }

        List<ChatMessage> messages = new ArrayList<>();
        for (String rawMessage : rawMessages) {
            try {
                messages.add(objectMapper.readValue(rawMessage, ChatMessage.class));
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Failed to deserialize message", exception);
            }
        }
        return messages;
    }

    public void deleteRoom(String roomId) {
        String normalizedRoom = normalizeRoomId(roomId);
        assertRoomExists(normalizedRoom);
        redisTemplate.delete(List.of(
                keys.roomMeta(normalizedRoom),
                keys.roomParticipants(normalizedRoom),
                keys.roomMessages(normalizedRoom)
        ));
        redisTemplate.opsForSet().remove(keys.roomsSet(), normalizedRoom);
    }

    public boolean roomExists(String roomId) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(keys.roomsSet(), normalizeRoomId(roomId)));
    }

    private void assertRoomExists(String roomId) {
        if (!roomExists(roomId)) {
            throw new RoomNotFoundException("Chat room '" + roomId + "' does not exist.");
        }
    }

    private String normalizeRoomId(String roomId) {
        return roomId == null ? "" : roomId.trim();
    }
}
