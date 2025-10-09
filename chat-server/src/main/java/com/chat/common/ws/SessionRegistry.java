package com.chat.common.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket으로 연결된 모든 사용자의 세션 정보를 등록하고 관리하는 중앙 저장소
 *세션ID -> WsEmitter 매핑을 관리
 * WebSocket 연결이 새로 생길 때마다 createEmitter()로 emitter를 만들고, 이를 emitters라는 맵에 저장
 *
 * 필드 분석
 * emitters: 세션 ID(String)를 Key로, 해당 세션에 대한 메시지 발신자인
 *
 *
 *
 *
 */
@Component
public class SessionRegistry {
    private final Map<String, WsEmitter> emitters = new ConcurrentHashMap<>();

    public WsEmitter createEmitter(String sessionId, WebSocketSession session) {
        var emitter = WsEmitter.of(sessionId, session);
        emitters.put(sessionId, emitter);
        return emitter;
    }

    public WsEmitter get(String sessionId){return emitters.get(sessionId);}

    public void cleanup(String sessionId) {
        var e = emitters.remove(sessionId);
        if (e != null) e.complete();
    }
}