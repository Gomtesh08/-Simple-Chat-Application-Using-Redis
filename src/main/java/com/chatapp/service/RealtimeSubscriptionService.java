package com.chatapp.service;

import com.chatapp.exception.RoomNotFoundException;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class RealtimeSubscriptionService {

    private final ChatService chatService;
    private final RedisMessageListenerContainer listenerContainer;
    private final RedisKeyService keys;

    private final Map<String, List<SseEmitter>> emittersByRoom = new ConcurrentHashMap<>();
    private final Map<String, MessageListener> listenersByRoom = new ConcurrentHashMap<>();

    public RealtimeSubscriptionService(ChatService chatService,
                                       RedisMessageListenerContainer listenerContainer,
                                       RedisKeyService keys) {
        this.chatService = chatService;
        this.listenerContainer = listenerContainer;
        this.keys = keys;
    }

    public SseEmitter subscribe(String roomId) {
        if (!chatService.roomExists(roomId)) {
            throw new RoomNotFoundException("Chat room '" + roomId + "' does not exist.");
        }

        SseEmitter emitter = new SseEmitter(0L);
        emittersByRoom.computeIfAbsent(roomId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        listenersByRoom.computeIfAbsent(roomId, this::registerListener);

        emitter.onCompletion(() -> removeEmitter(roomId, emitter));
        emitter.onTimeout(() -> removeEmitter(roomId, emitter));
        emitter.onError(ignored -> removeEmitter(roomId, emitter));
        return emitter;
    }

    private MessageListener registerListener(String roomId) {
        MessageListener listener = (Message message, byte[] pattern) -> {
            String payload = new String(message.getBody());
            List<SseEmitter> emitters = emittersByRoom.get(roomId);
            if (emitters == null) {
                return;
            }
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().name("message").data(payload));
                } catch (IOException exception) {
                    removeEmitter(roomId, emitter);
                }
            }
        };

        listenerContainer.addMessageListener(listener, new ChannelTopic(keys.roomChannel(roomId)));
        return listener;
    }

    private void removeEmitter(String roomId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByRoom.get(roomId);
        if (emitters == null) {
            return;
        }

        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByRoom.remove(roomId);
            MessageListener listener = listenersByRoom.remove(roomId);
            if (listener != null) {
                listenerContainer.removeMessageListener(listener, new ChannelTopic(keys.roomChannel(roomId)));
            }
        }
    }
}
