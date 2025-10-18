package com.chat.chat;

import com.chat.common.ws.WsEmitter;
import com.chat.chat.model.ChatInbound;
import com.chat.chat.model.ChatOutbound;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

/**
 * 이번 단계: CHAT만 처리. 서버 콘솔 출력 + 간단 ACK.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageRouter {

    public Mono<Void> route(ChatInbound inbound, WebSocketSession session, WsEmitter out) {
        var type = inbound.getType();
        if (type == null) return Mono.empty();

        return switch (type) {
            case START -> {
                log.info("[WS:{}] START", session.getId());
                // 필요하면 ACK 전송
                out.emit(ChatOutbound.system("SESSION_STARTED"));
                yield Mono.empty();
            }
            case CHAT -> {
                log.info("[WS:{}] CHAT: {}", session.getId(), inbound.getText());
                // 필요하면 ACK 전송(원치 않으면 이 줄 삭제)
                out.emit(ChatOutbound.system("RECEIVED:" + inbound.getText()));
                yield Mono.empty();
            }
            case PING -> {
                out.emit(ChatOutbound.pong(System.currentTimeMillis()));
                yield Mono.empty();
            }
            // 기존 타입은 이번 단계에서 미사용
            case AUDIO_CHUNK, STOP -> Mono.empty();
        };
    }

}