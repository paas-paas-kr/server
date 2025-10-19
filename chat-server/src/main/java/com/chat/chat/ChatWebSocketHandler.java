package com.chat.chat;

import com.chat.common.Lang;
import com.chat.common.constants.MessageType;
import com.chat.common.ws.SessionRegistry;
import com.chat.chat.model.ChatInbound;
import com.chat.common.json.JsonUtils;
import com.chat.trans.NaverPapagoTransClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

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
    private final NaverPapagoTransClient transClient;
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        var emitter = registry.createEmitter(session.getId(), session);

        //map: 동기 변환 / flatMap: 비동기&순서 무관 / concatMap: 비동기 & 순서 보장
        // concatMap은 내부는 비동기적으로 처리되지만, 입력 순서대로 결과 순서도 보장한다
        var inbound = session.receive()
                .map(msg -> msg.getPayloadAsText())
                .map(JsonUtils::fromJsonInbound)
                .doOnNext(in -> log.info("[WS:{}] inbound: {}", session.getId(), in))
                .concatMap((ChatInbound in)->{
                    Mono<Void> originalFlow = router.route(in,session,emitter); // 비동기 -> Publisher 필요

                    String source = Lang.mapCsrToPapago(in.getLang());
                    String target = "ko";
                    String text = in.getText();

                    Mono<Void> translatedFlow = transClient.translate(source, target, text) //비동기 I/O
                            .map(t-> copyAsTrans(in, t))// 동기 변환: String -> ChatInbound
                            .flatMap(in2-> router.route(in2, session,emitter))//비동기 라우트 -> flatmap
                            .onErrorResume(e-> Mono.empty());

                    //originalFlow가 완료된 뒤에 translatedFlow를 실행한다.
                    //반환 타입은 translatedFlow의 타입
                    //원문 라우팅을 끝내고, 번역 실행
                    return originalFlow.then(translatedFlow);
                })
                .doFinally((SignalType sig) -> {
                    log.info("[WS:{}] closed: {}", session.getId(), sig);
                    registry.cleanup(session.getId());
                });

        var outbound = session.send(emitter.flux());
        return Mono.when(inbound, outbound);
    }



    private ChatInbound copyAsTrans(ChatInbound in, String translated){
        return new ChatInbound(MessageType.TRANS,translated, in.getUserId(), in.getLang());
    }

}