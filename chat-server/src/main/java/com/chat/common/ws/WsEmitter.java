package com.chat.common.ws;

import com.chat.chat.model.ChatOutbound;
import com.chat.common.json.JsonUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;


/**
 * 특정 WebSocket 클라이언트 한 명에게 메시지를 보내는 역할을 전담
 * 복잡한 리액티브 스트림(Sink)을 내부에 감추고, 외부에서는 emit()나 complete() 같은 간단한 메서드만 호출하여 특정 클라이언트와 통신할 수 있도록 편리한 인터페이스 제공
 *
 * 이 클래스는 특정 WebSocket 클라이언트 한 명에게 메시지를 보내는(emit) 역할을 담당합니다.
 * 내부적으로 Project Reactor의 Sinks API를 사용하여 메시지를 반응형 스트림(Flux)으로 처리합니다.
 */
@RequiredArgsConstructor
@Getter
public class WsEmitter {

    private final String sessionId;
    private final WebSocketSession session;
    private final Sinks.Many<WebSocketMessage> sink;
    private final java.util.concurrent.ConcurrentHashMap<String, Object> attributes = new java.util.concurrent.ConcurrentHashMap<>();

    public static WsEmitter of(String sessionId, WebSocketSession session) {
        var sink = Sinks.many()
                .unicast()
                .<WebSocketMessage>onBackpressureBuffer();
        return new WsEmitter(sessionId, session, sink);
    }

    public Flux<WebSocketMessage> flux() {
        return sink.asFlux();
    }

    public void emitText(String json) {
        var msg = session.textMessage(json);
        System.out.println("-----------------------------------------------");
        System.out.println(json);
        System.out.println("-----------------------------------------------");
        sink.tryEmitNext(msg);
    }

    public void emit(ChatOutbound outbound) {
        String json = JsonUtils.toJson(outbound);
        System.out.println("-----------------------------------------------");
        System.out.println(json);
        System.out.println("-----------------------------------------------");
        sink.tryEmitNext(session.textMessage(json));
    }

    public void emitBinary(byte[] bytes) {
        sink.tryEmitNext(session.binaryMessage(f -> f.wrap(bytes)));
    }

    public void complete() {
        sink.tryEmitComplete();
    }

    public <T> void setAttribute(String key, T value) {
        if (value == null) attributes.remove(key);
        else attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, Class<T> type) {
        Object v = attributes.get(key);
        if (v == null) return null;
        if (!type.isInstance(v)) return null;
        return (T) v;
    }

    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    public void clearAttributes() {
        attributes.clear();
    }

}