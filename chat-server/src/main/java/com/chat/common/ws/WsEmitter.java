package com.chat.common.ws;

import com.chat.chat.model.ChatOutbound;
import com.chat.common.json.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;


/**
 * 특정 WebSocket 클라이언트 한 명에게 메시지를 보내는 역할을 전담
 * 복잡한 리액티브 스트림(Sink)을 내부에 감추고, 외부에서는 emit()나 complete() 같은 간단한 메서드만 호출하여 특정 클라이언트와 통신할 수 있도록 편리한 인터페이스 제공
 *
 * 필드 분석
 * sessionId: Emitter가 담당하는 클라이언트의 고유 세션 ID, 식별 및 로깅 용도
 * session: Spring WebFlux가 제공하는 원본 WebSocketSession 객체
 * -> 전송할 텍스트를 WebSocketMessage 객체로 포장(session.textMessage(...)) 하는 데 사용
 * sink: SessionRegistry에 생성된 Sink 객체
 * -> sink는 메시지가 실제로 흘러가는 파이프라인 역할
 *
 *메서드 분석
 *
 *
 *
 *
 * emit(WsOutbound outbound)
 *
 *
 */
@RequiredArgsConstructor
public class WsEmitter {
    private final String sessionId;
    private final WebSocketSession session;
    private final Sinks.Many<WebSocketMessage> sink;

    public static WsEmitter of(String sessionId, WebSocketSession session){
        var sink = Sinks.many().unicast().<WebSocketMessage>onBackpressureBuffer();
        return new WsEmitter(sessionId, session, sink);
    }

    public Flux<WebSocketMessage> flux() {
        return sink.asFlux();
    }

    public void emitText(String json){
        var msg= session.textMessage(json);
        sink.tryEmitNext(msg);
    }
    public void emit(ChatOutbound outbound) {
        // 1. 자바 객체를 JSON 문자열로 변환
        String json = JsonUtils.toJson(outbound);
        // 2. JSON을 WebSocketMessage로 포장 후 Sink에 주입
        sink.tryEmitNext(session.textMessage(json));
    }

    public void emitBinary(byte[] bytes){
        sink.tryEmitNext(session.binaryMessage(f->f.wrap(bytes)));
    }


    public void complete() {
        sink.tryEmitComplete();
    }
}