package com.chatapp.controller;

import com.chatapp.dto.ApiResponse;
import com.chatapp.dto.ChatHistoryResponse;
import com.chatapp.dto.CreateChatRoomRequest;
import com.chatapp.dto.CreateRoomResponse;
import com.chatapp.dto.JoinRoomRequest;
import com.chatapp.dto.SendMessageRequest;
import com.chatapp.service.ChatService;
import com.chatapp.service.RealtimeSubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/chatapp")
public class ChatController {

    private final ChatService chatService;
    private final RealtimeSubscriptionService realtimeSubscriptionService;

    public ChatController(ChatService chatService, RealtimeSubscriptionService realtimeSubscriptionService) {
        this.chatService = chatService;
        this.realtimeSubscriptionService = realtimeSubscriptionService;
    }

    @PostMapping("/chatrooms")
    public ResponseEntity<CreateRoomResponse> createRoom(@Valid @RequestBody CreateChatRoomRequest request) {
        String roomId = chatService.createRoom(request.roomName());
        return ResponseEntity.ok(new CreateRoomResponse(
                "Chat room '" + roomId + "' created successfully.",
                roomId,
                "success"
        ));
    }

    @PostMapping("/chatrooms/{roomId}/join")
    public ResponseEntity<ApiResponse> joinRoom(@PathVariable String roomId, @Valid @RequestBody JoinRoomRequest request) {
        chatService.joinRoom(roomId, request.participant());
        return ResponseEntity.ok(new ApiResponse(
                "User '" + request.participant() + "' joined chat room '" + roomId + "'.",
                "success"
        ));
    }

    @PostMapping("/chatrooms/{roomId}/messages")
    public ResponseEntity<ApiResponse> sendMessage(@PathVariable String roomId,
                                                   @Valid @RequestBody SendMessageRequest request) {
        chatService.sendMessage(roomId, request.participant(), request.message());
        return ResponseEntity.ok(new ApiResponse("Message sent successfully.", "success"));
    }

    @GetMapping("/chatrooms/{roomId}/messages")
    public ResponseEntity<ChatHistoryResponse> getMessages(@PathVariable String roomId,
                                                           @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(new ChatHistoryResponse(chatService.getMessages(roomId, limit)));
    }

    @GetMapping(value = "/chatrooms/{roomId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String roomId) {
        return realtimeSubscriptionService.subscribe(roomId);
    }

    @DeleteMapping("/chatrooms/{roomId}")
    public ResponseEntity<ApiResponse> deleteRoom(@PathVariable String roomId) {
        chatService.deleteRoom(roomId);
        return ResponseEntity.ok(new ApiResponse(
                "Chat room '" + roomId + "' deleted successfully.",
                "success"
        ));
    }
}
