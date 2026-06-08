package com.adoptar.controller;

import com.adoptar.dto.request.EnviarMensajeRequest;
import com.adoptar.dto.request.IniciarChatRequest;
import com.adoptar.entity.User;
import com.adoptar.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/iniciar")
    public ResponseEntity<?> iniciarChat(@AuthenticationPrincipal User user,
                                         @RequestBody IniciarChatRequest req) {
        return ResponseEntity.ok(chatService.iniciarChat(user, req));
    }

    @GetMapping
    public ResponseEntity<?> getMisChats(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.getMisChats(user));
    }

    @GetMapping("/{chatId}/mensajes")
    public ResponseEntity<?> getMensajes(@AuthenticationPrincipal User user,
                                         @PathVariable Long chatId) {
        return ResponseEntity.ok(chatService.getMensajes(chatId, user));
    }

    @PostMapping("/{chatId}/mensajes")
    public ResponseEntity<?> enviarMensaje(@AuthenticationPrincipal User user,
                                           @PathVariable Long chatId,
                                           @RequestBody EnviarMensajeRequest req) {
        return ResponseEntity.ok(chatService.enviarMensaje(chatId, user, req.getContenido()));
    }

    @GetMapping("/no-leidos")
    public ResponseEntity<?> getNoLeidos(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.getNoLeidos(user));
    }
}
