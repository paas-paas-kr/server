package com.chat.chat;

import com.chat.common.ws.SessionRegistry;
import com.chat.chat.model.ChatInbound;
import com.chat.common.json.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

/**
 * SessionRegistry: 현재 연결된 모든 WebSocket 세션 정보를 관리하는 저장소
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler implements WebSocketHandler {

    private final SessionRegistry registry;
    private final ChatMessageRouter router;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        var emitter = registry.createEmitter(session.getId(), session);

        var inbound = session.receive()
                .map(msg -> msg.getPayloadAsText())
                .map(JsonUtils::fromJsonInbound)
                .doOnNext(in -> log.info("[WS:{}] inbound: {}", session.getId(), in))
                .concatMap((ChatInbound in) -> router.route(in, session, emitter))
                .doFinally(sig -> {
                    log.info("[WS:{}] closed: {}", session.getId(), sig);
                    registry.cleanup(session.getId());
                });

        var outbound = session.send(emitter.flux());
        return Mono.when(inbound, outbound);
    }

}