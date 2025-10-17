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
 * 이 클래스는 특정 WebSocket 클라이언트 한 명에게 메시지를 보내는(emit) 역할을 담당합니다.
 * 내부적으로 Project Reactor의 Sinks API를 사용하여 메시지를 반응형 스트림(Flux)으로 처리합니다.
 * 비유하자면, 특정 클라이언트에게 연결된 '메시지 파이프' 또는 '방송 송출기'입니다.
 */
@RequiredArgsConstructor
public class WsEmitter {

    // 이 Emitter가 담당하는 웹소켓 세션의 고유 ID
    private final String sessionId;
    // 실제 클라이언트와의 연결을 나타내는 Spring의 WebSocketSession 객체
    private final WebSocketSession session;
    // 메시지를 주입하고 Flux로 변환할 수 있는 반응형 스트림의 '소스(Source)' 또는 '입구'
    private final Sinks.Many<WebSocketMessage> sink;

    /**
     * WsEmitter 인스턴스를 생성하는 정적 팩토리 메소드(Static Factory Method)입니다.
     * 생성자를 직접 호출하는 것보다 객체 생성의 의도를 명확하게 표현할 수 있습니다.
     *
     * @param sessionId 고유 세션 ID
     * @param session   웹소켓 연결 객체
     * @return 완전히 초기화된 WsEmitter 인스턴스
     */
    public static WsEmitter of(String sessionId, WebSocketSession session) {
        // 1. 메시지를 주입할 Sink를 생성합니다.
        var sink = Sinks.many() // 0개 이상의 메시지를 발행할 수 있는 Sink
                .unicast()       // 단 하나의 구독자(Subscriber)만 허용합니다. (웹소켓 세션당 처리기가 하나이므로 적합)
                .<WebSocketMessage>onBackpressureBuffer(); // 구독자가 메시지를 처리하는 속도보다 발행 속도가 빠를 때,
        // 처리하지 못한 메시지를 내부 버퍼에 쌓아두는 전략(Backpressure)을 사용합니다.
        // 이렇게 하면 메시지 유실을 방지할 수 있습니다.

        // 2. Lombok이 만들어준 생성자를 통해 WsEmitter 인스턴스를 생성하여 반환합니다.
        return new WsEmitter(sessionId, session, sink);
    }

    /**
     * 내부 Sink를 구독 가능한 Flux<WebSocketMessage> 형태로 노출합니다.
     * 이 Flux는 웹소켓 핸들러가 구독(subscribe)하여, Sink에 주입되는 메시지들을 실제로 클라이언트에게 전송하는 역할을 합니다.
     * 즉, 이 파이프의 '출구'를 제공합니다.
     *
     * @return Sink에 주입된 메시지들이 흘러나오는 Flux 스트림
     */
    public Flux<WebSocketMessage> flux() {
        return sink.asFlux();
    }

    /**
     * (사용되지 않는 메소드 예시)
     * 순수한 JSON 문자열을 클라이언트에게 전송합니다.
     *
     * @param json 전송할 JSON 형식의 문자열
     */
    public void emitText(String json) {
        // 1. WebSocketSession을 이용해 문자열을 TextMessage 객체로 변환합니다.
        var msg = session.textMessage(json);
        // 2. 변환된 메시지를 Sink에 주입(emit)하여 스트림으로 흘려보냅니다.
        // tryEmitNext는 non-blocking 방식으로, 실패 시 에러를 던지지 않고 상태를 반환합니다.
        sink.tryEmitNext(msg);
    }

    /**
     * 전송할 데이터 객체(ChatOutbound)를 받아 JSON으로 변환 후 클라이언트에게 전송합니다.
     * 애플리케이션의 다른 부분에서 이 메소드를 호출하여 특정 클라이언트에게 메시지를 보냅니다.
     *
     * @param outbound 전송할 데이터가 담긴 자바 객체
     */
    public void emit(ChatOutbound outbound) {
        // 1. 유틸리티 클래스를 사용해 자바 객체를 JSON 문자열로 직렬화(serialize)합니다.
        String json = JsonUtils.toJson(outbound);
        // 2. JSON 문자열을 WebSocketMessage(TextMessage)로 포장한 뒤 Sink에 주입합니다.
        sink.tryEmitNext(session.textMessage(json));
    }

    /**
     * 바이너리 데이터(예: 오디오, 이미지)를 클라이언트에게 전송합니다.
     *
     * @param bytes 전송할 바이트 배열
     */
    public void emitBinary(byte[] bytes) {
        // session.binaryMessage를 통해 byte[]를 BinaryMessage 객체로 포장하고 Sink에 주입합니다.
        // f.wrap(bytes)는 내부 버퍼에 바이트 배열을 복사하는 효율적인 방식입니다.
        sink.tryEmitNext(session.binaryMessage(f -> f.wrap(bytes)));
    }

    /**
     * 메시지 스트림의 정상적인 종료를 알립니다.
     * 웹소켓 연결이 끊어졌을 때 호출되어, 이 Emitter와 관련된 모든 리소스를 정리하도록 신호를 보냅니다.
     */
    public void complete() {
        // Sink에 '완료' 신호를 보내 스트림을 닫습니다.
        // 이후에는 더 이상 tryEmitNext를 통해 메시지를 주입할 수 없습니다.
        sink.tryEmitComplete();
    }
}