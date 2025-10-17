package com.chat.common.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Component: 이 클래스를 스프링(Spring)이 관리하는 빈(Bean)으로 등록합니다.
 * 다른 컴포넌트에서 의존성 주입(DI)을 통해 이 클래스의 단일 인스턴스를 사용할 수 있습니다.
 *
 * 활성화된 모든 웹소켓 세션(WsEmitter)을 관리하는 중앙 저장소(Registry) 역할을 하는 클래스입니다.
 * 세션 ID를 키(key)로 사용하여 각 세션의 Emitter에 쉽게 접근하고, 생성 및 제거를 담당합니다.
 * 멀티스레드 환경에서 안전하게 동작하도록 설계되었습니다.
 */
@Component
public class SessionRegistry {

    /**
     * 활성화된 모든 WsEmitter 인스턴스를 저장하는 맵입니다.
     * - Key (String): 각 웹소켓 세션을 고유하게 식별하는 세션 ID입니다.
     * - Value (WsEmitter): 실제 데이터 전송을 담당하는 Emitter 객체입니다.
     * <p>
     * [왜 ConcurrentHashMap을 사용하는가?]
     * 웹소켓 서버는 다수의 클라이언트 연결을 동시에 여러 스레드에서 처리합니다.
     * ConcurrentHashMap은 여러 스레드가 동시에 이 맵에 접근하여 데이터를 추가, 조회, 삭제하더라도
     * 데이터의 일관성을 보장해주는(thread-safe) 고성능 자료구조이므로 사용됩니다.
     */
    private final Map<String, WsEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 새로운 웹소켓 연결을 위한 WsEmitter를 생성하고 레지스트리에 등록합니다.
     * 이 메소드는 클라이언트와 웹소켓 연결이 처음 수립되었을 때 호출됩니다.
     *
     * @param sessionId 고유한 세션 식별자입니다.
     * @param session   스프링 웹소켓 프레임워크가 생성해준 원본 WebSocketSession 객체입니다.
     * @return 생성되고 등록된 새로운 WsEmitter 인스턴스를 반환합니다.
     */
    public WsEmitter createEmitter(String sessionId, WebSocketSession session) {
        // 주어진 세션 ID와 WebSocketSession을 사용하여 WsEmitter 인스턴스를 생성합니다.
        var emitter = WsEmitter.of(sessionId, session);
        // 생성된 emitter를 sessionId를 키로 하여 맵에 저장(등록)합니다.
        emitters.put(sessionId, emitter);
        // 방금 생성한 emitter를 호출한 쪽으로 반환합니다.
        return emitter;
    }

    /**
     * 주어진 세션 ID에 해당하는 WsEmitter를 레지스트리에서 조회합니다.
     *
     * @param sessionId 조회하고자 하는 세션의 ID입니다.
     * @return 해당 ID를 가진 WsEmitter 객체를 반환합니다. 만약 존재하지 않으면 null을 반환합니다.
     */
    public WsEmitter get(String sessionId) {
        return emitters.get(sessionId);
    }

    /**
     * 특정 세션의 연결이 종료되었을 때, 해당 세션을 레지스트리에서 제거하고 관련 리소스를 정리합니다.
     * 이 메소드는 웹소켓 연결이 끊어지거나, 타임아웃이 발생했을 때 호출됩니다.
     *
     * @param sessionId 정리할 세션의 ID입니다.
     */
    public void cleanup(String sessionId) {
        // 맵에서 sessionId에 해당하는 emitter를 '원자적으로(atomically)' 제거하고,
        // 제거된 emitter를 반환받습니다. (제거와 반환이 동시에 일어남)
        var e = emitters.remove(sessionId);

        // 만약 맵에 해당 sessionId가 존재해서 emitter가 성공적으로 제거되었다면 (null이 아니라면)
        if (e != null) {
            // 해당 emitter의 complete() 메소드를 호출하여 스트림을 정상적으로 종료시키고
            // 관련된 모든 리소스를 해제하도록 합니다.
            e.complete();
        }
    }
}