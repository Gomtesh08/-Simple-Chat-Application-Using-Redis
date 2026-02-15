package com.chatapp.service;

import com.chatapp.dto.ChatMessage;
import com.chatapp.exception.DuplicateRoomException;
import com.chatapp.exception.RoomNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private SetOperations<String, String> setOperations;
    @Mock
    private HashOperations<String, String, String> hashOperations;
    @Mock
    private ListOperations<String, String> listOperations;

    private ChatService chatService;
    private RedisKeyService keys;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        keys = new RedisKeyService();
        objectMapper = new ObjectMapper();
        chatService = new ChatService(redisTemplate, keys, objectMapper);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
    }

    @Test
    void createRoomShouldStoreMetadataWhenRoomDoesNotExist() {
        when(setOperations.isMember(keys.roomsSet(), "general")).thenReturn(false);

        chatService.createRoom("general");

        verify(setOperations).add(keys.roomsSet(), "general");
        verify(hashOperations).put(keys.roomMeta("general"), "name", "general");
        verify(hashOperations).put(anyString(), anyString(), anyString());
    }

    @Test
    void createRoomShouldThrowWhenDuplicateExists() {
        when(setOperations.isMember(keys.roomsSet(), "general")).thenReturn(true);

        assertThrows(DuplicateRoomException.class, () -> chatService.createRoom("general"));
        verify(setOperations, never()).add(anyString(), anyString());
    }

    @Test
    void joinRoomShouldAddParticipantToSet() {
        when(setOperations.isMember(keys.roomsSet(), "general")).thenReturn(true);

        chatService.joinRoom("general", "guest_user");

        verify(setOperations).add(keys.roomParticipants("general"), "guest_user");
    }

    @Test
    void sendMessageShouldThrowForNonExistentRoom() {
        when(setOperations.isMember(keys.roomsSet(), "unknown")).thenReturn(false);

        assertThrows(RoomNotFoundException.class,
                () -> chatService.sendMessage("unknown", "guest_user", "hello"));
        verify(redisTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    void getMessagesShouldReturnLastNMessages() throws Exception {
        ChatMessage msg1 = new ChatMessage("guest_user", "hello", "2024-01-01T10:00:00Z");
        ChatMessage msg2 = new ChatMessage("another_user", "hi", "2024-01-01T10:01:00Z");
        when(setOperations.isMember(keys.roomsSet(), "general")).thenReturn(true);
        when(listOperations.range(keys.roomMessages("general"), -2, -1)).thenReturn(List.of(
                objectMapper.writeValueAsString(msg1),
                objectMapper.writeValueAsString(msg2)
        ));

        List<ChatMessage> messages = chatService.getMessages("general", 2);

        assertEquals(2, messages.size());
        assertEquals("hello", messages.get(0).message());
        assertEquals("hi", messages.get(1).message());
        verify(setOperations, atLeastOnce()).isMember(keys.roomsSet(), "general");
    }
}
