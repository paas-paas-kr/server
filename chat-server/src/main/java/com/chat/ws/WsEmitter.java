package com.chat.ws;

import com.chat.model.WsOutbound;
import com.chat.util.JsonUtils;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public class WsEmitter {
    private final String sessionId;
    private final WebSocketSession session;
    private final Sinks.Many<WebSocketMessage> sink;

    public WsEmitter(String sessionId, WebSocketSession session, Sinks.Many<WebSocketMessage> sink) {
        this.sessionId = sessionId;
        this.session = session;
        this.sink = sink;
    }

    public Flux<WebSocketMessage> flux() {
        return sink.asFlux();
    }

    public void send(WsOutbound outbound) {
        String json = JsonUtils.toJson(outbound);
        sink.tryEmitNext(session.textMessage(json));
    }

    public void dispose() {
        sink.tryEmitComplete();
    }
}