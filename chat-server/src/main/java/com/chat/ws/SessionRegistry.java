package com.chat.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 세션별로 outbound Sink를 보관.
 */
@Component
public class SessionRegistry {
    private final Map<String, WsEmitter> emitters = new ConcurrentHashMap<>();

    public WsEmitter createEmitter(String sessionId, WebSocketSession session) {
        var sink = Sinks.many().unicast().<org.springframework.web.reactive.socket.WebSocketMessage>onBackpressureBuffer();
        var emitter = new WsEmitter(sessionId, session, sink);
        emitters.put(sessionId, emitter);
        return emitter;
    }

    public void cleanup(String sessionId) {
        var e = emitters.remove(sessionId);
        if (e != null) e.dispose();
    }
}