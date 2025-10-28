package com.chat.chat;

import com.chat.common.Lang;
import com.chat.common.constants.MessageType;
import com.chat.common.ws.SessionRegistry;
import com.chat.chat.model.ChatInbound;
import com.chat.common.json.JsonUtils;
import com.chat.common.ws.WsEmitter;
import com.chat.conversation.service.ConversationService;
import com.chat.pipeline.LlmFirstRagOrchestrator;
import com.chat.rag.model.Citation;
import com.chat.rag.model.SearchPlan;
import com.chat.trans.NaverPapagoTransClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.util.List;
import java.util.Map;


@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler implements WebSocketHandler {

    private final SessionRegistry registry;  //WebSocket 세션(연결)을 관리하는 레지스트리
    private final ChatMessageRouter router;  // 수신된 메시지를 적절한 비즈니스 로직으로 라우팅
    private final NaverPapagoTransClient transClient; //Papago API 번역 클라이언트 (비동기 Mono 반환)
    private final LlmFirstRagOrchestrator rag; //Rag 및 LLM 오케스트레이터 (비동기 Mono 반환)
    private final ConversationService conversationService; //DB 저장을 위한 서비스 주입
    /**
     * WebSocket 연결이 수립될 때 호출되는 메인 메서드
     * @param session 현재 연결된 WebSocket 세션
     * @return Mono<Void> 작업이 비동기적으로 완료됨을 나타냄
     */
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        // 1. Emitter 생성: 클라이언트에게 메시지를 보내는 통로(Flux)를 생성하고 레지스트리에 등록
        WsEmitter emitter = registry.createEmitter(session.getId(), session);
        // 2. Inbound(수신) 파이프라인 정의 : 클라이언트로부터 메시지를 받았을 때의 처리 흐름
        //map: 동기 변환 / flatMap: 비동기&순서 무관 / concatMap: 비동기 & 순서 보장
        // concatMap은 내부는 비동기적으로 처리되지만, 입력 순서대로 결과 순서도 보장한다
        // Mono(값 0 or 1)일때는 flatMap과 concatMap의 차이는 없다.
        // concatMap는 flux로 들어오는 원소의 순서를 보장하는 것이지만 Mono처럼 원소가 최대 한개인 경우에는 보장할 순서가 없다.
        // flatMap은 각 원소들을 병렬로 실행, 결과는 먼저 완료된 순서대로 나온다.
        // concatMap과 flatMap은 내부 로직이 비동기적으로 흐르는 것이고 map은 내부 로직도 동기적으로 흐르는 것
        //(Flux<WebSocketMessage>) 클라이언트 메시지 흐름
        var inbound = session.receive()
                // (Flux<String>) 메시지를 순수 텍스트 (JSON)로 변환 (동기 map)
                .map(msg -> msg.getPayloadAsText())
                // (Flux<ChatInbound>) JSON을 ChatInbound 객체로 변환 (동기 map)
                .map(JsonUtils::fromJsonInbound)
                // 수신된 객체 로깅
                .doOnNext(in -> log.info("[WS:{}] inbound: {}", session.getId(), in))
                // concatMap: 메시지 순서 보장
                // 만약 flatMap을 쓰면, 클라이언트가 A1, A2, A3 메시지를 연달아 보냈을 때 A2의 RAG가 더 빨리 끝나면 A2가 먼저 응답할 수 있음
                // concatMap은 A1 메시지에 대한 모든 비동기 작업(아래의 then 체인)이 "완료"될 때까지 B 메시지 처리를 "시작"조차 안 함
                // 여기서 순서 보장은 같은 클라이언트에 대한 순서 보장이다
                // 클라이언트 A가 접속하면, A전용 handle 메서드가 실행되고, A전용 concatMap이 A의 메시지(A1, A2, A3)을 순서대로 처리
                // 클라이언트 B가 동시에 접속하면, B전용 handle 메서드가 별도로 실행되고, B전용 concatMap이 B의 메시지(B1,B2)를 순서대로 처리
                //즉 클라이언트 A와 클라이언트 B의 질문은 동시에 처리
                .concatMap((ChatInbound in)->{
                    //여기 안에서는 코드가 비동기적으로 실행
                    // concatMap안에서 순서가 필요할 때는
                    //return originalFlow.then(translatedFlow).then(ragAndTranslateFlow); 와 같이 순서대로 실행되게 할 수 있음
                    //.then()은 앞의 작업이 뭘 줬든 데이터는 무시하고, 끝났다는 신호만 받아서 다음 작업을 함( 데이터 x,순서 o)

                    // Flow 1: 원본 메시지 라우팅 (비동기)
                    // router.route는 Mono<Void> (비동기 작업)를 반환해야 함
                    Mono<Void> originalFlow = router.route(in, session, emitter);

                    // 번역 및 RAG에 필요한 공통 변수 설정
                    String sourceLang = Lang.mapCsrToPapago(in.getLang()); // 예: "en"
                    String targetLang = "ko"; // RAG/LLM은 한국어로 처리
                    String text = in.getText(); // 사용자의 원본 텍스트
                    String traceId = session.getId(); // 트레이싱 ID로 세션 ID 사용
                    in.setUserId(session.getId());
                    String roomId = in.getRoomId();
                    System.out.println(in);
                    // 번역 결과 캐시: Papago API 호출을 1번만 하기 위함
                    // 아래 translatedFlow와 ragAndTranslateFlow가 모두 '한국어 번역본'을 필요로 함.
                    // .cache()가 없으면 Papago API가 2번 호출됨.
                    // .cache()를 쓰면 첫 번째 구독자가 API를 호출하고, 그 결과를 저장했다가 두 번째 구독자에게 공유함.
                    Mono<String> koMono = transClient.translate(sourceLang, targetLang, text).cache();

                    // Flow 2: 단순 번역 메시지 라우팅 (비동기)
                    // (사용자에게 "번역: [번역결과]"를 보내주는 흐름)
                    Mono<Void> translatedFlow = koMono
                            .map(t -> copyAsTrans(in, t)) // (동기 map) 번역된 텍스트(t)로 새 ChatInbound 객체 생성
                            .flatMap(in2 -> router.route(in2, session, emitter)) // (비동기 flatMap) 번역본을 라우팅
                            .onErrorResume(e -> Mono.empty()); // 이 흐름에서 에러나도 전체를 중단시키지 않고 무시

                    // Flow 3: RAG 실행 -> LLM 답변 -> 역번역 -> 클라이언트 전송 (비동기)
                    Mono<Void> ragAndTranslateFlow = koMono
                            .flatMap(koUserText -> { // (비동기 flatMap) 캐시된 한국어 텍스트를 받음
                                // 2a. RAG + LLM 실행 (비동기)
                                // rag.run이 LLM의 최종 한국어 답변(String)을 Mono로 반환함
                                // (emitter는 RAG/LLM의 중간 스트리밍 데이터를 보내는 데 사용될 수 있음)
                                return rag.run(koUserText, emitter);
                            })
                            .doOnNext(t -> System.out.println("text ="+ t)) // LLM의 최종 한국어 답변 로깅
                            .flatMap(llmKoAnswer -> { // (비동기 flatMap) LLM의 한국어 답변을 받음
                                // 2b. Papago 역번역 (비동기)
                                // LLM의 한국어 답변("ko")을 사용자의 원래 언어(sourceLang, 예: "en")로 다시 번역
                                return transClient.translate(targetLang, sourceLang, llmKoAnswer);
                            })
                            .doOnNext(translatedAnswer -> { // 최종 번역된 답변(예: "en")을 받음
                                // 2c. 클라이언트에 최종 답변 전송
                                // emitter를 통해 "original_text" 이벤트로 최종 LLM 답변을 클라이언트에 전송
                                emitter.emitText(JsonUtils.toJson(Map.of(
                                        "type", "nlp-stream",
                                        "event", "original_text", // 원본 텍스트 이벤트
                                        "data", Map.of("text", translatedAnswer),
                                        "traceId", traceId
                                )));
                            })
                            .doOnNext(translatedAnswer->{
                                // --- [수정 3: Fire-and-Forget으로 '질문/답변' 동시 저장] ---
                                // LLM 답변을 받은 이 시점에 '질문'과 '답변'을 모두 안다.
                                // .subscribe()를 호출하여 DB 저장을 백그라운드로 보내고 기다리지 않는다.

                                conversationService.createMessage(text, translatedAnswer, roomId != null ? roomId : session.getId())
                                        .subscribe(
                                                // 성공 시 로그 (저장된 메시지 ID 등)
                                                savedMessage -> log.info("[WS:{}] 메시지 저장 성공: roomId={}, messageId={}", session.getId(), roomId, savedMessage.getId()),
                                                // 실패 시 에러 로그
                                                err -> log.error("[WS:{}] '질문/답변' DB 저장 실패: roomId={}, error={}", session.getId(), roomId, err.getMessage())
                                        );
                            })
                            .then();

                    // ★ 3개의 비동기 Flow 순서 보장
                    // .then()은 데이터(onNext)는 무시하고, 앞선 Mono가 완료(onComplete) 신호를 보내야
                    // 그 다음 Mono를 구독(실행) 시키는 순서 보장 오퍼레이터.
                    // 1. originalFlow (원본 저장)가 끝날 때까지 기다린다.
                    // 2. 끝나면, translatedFlow (단순 번역 전송)를 실행하고 끝날 때까지 기다린다.
                    // 3. 끝나면, ragAndTranslateFlow (RAG+LLM)를 실행하고 끝날 때까지 기다린다.
                    // 이 3개가 모두 끝나야, concatMap이 비로소 이번 메시지(A) 처리 완료로 간주함.
                    return originalFlow.then(translatedFlow).then(ragAndTranslateFlow);
                })
                .doFinally((SignalType sig) -> { // (부수 효과) Inbound 스트림이 *최종* 종료될 때
                    // .concatMap은 메시지 1개마다 실행되지만, .doFinally는 연결이 살아있는 동안에는 실행 안 됨
                    // (정상 종료, 에러, 취소 등 모든 경우)
                    log.info("[WS:{}] closed: {}", session.getId(), sig);
                    // 레지스트리에서 세션 및 emitter 정리 (메모리 누수 방지)
                    registry.cleanup(session.getId());
                });

        // 3. Outbound(송신) 파이프라인 정의: 서버에서 클라이언트로 메시지를 "보내는" 흐름
        // session.send()에게 우리가 만든 emitter의 Flux(스트림)를 연결함.
        // emitter.emitText()로 보낸 모든 메시지가 이 파이프라인을 타고 클라이언트로 전송됨.
        var outbound = session.send(emitter.flux());

        // 4. Inbound와 Outbound를 동시에 실행
        // Mono.when은 두 비동기 작업(inbound, outbound)이 모두 완료될 때까지 기다림.
        // WebSocket 연결은 양방향이므로, 둘 다 실행되어야 함.
        // 이 handle 메서드는 '연결이 끊어질 때' 비로소 완료(return)됨.
        return Mono.when(inbound, outbound);
    }

    /**
     * 수신된 ChatInbound 객체를 기반으로 "번역" 타입의 새 객체를 복사하여 생성하는 헬퍼 메서드.
     * @param in 원본 ChatInbound
     * @param translated 번역된 텍스트
     * @return type이 MessageType.TRANS로 설정된 새 ChatInbound 객체
     */
    private ChatInbound copyAsTrans(ChatInbound in, String translated){
        // 원본의 userId와 lang을 유지하되, text와 type을 변경
        return new ChatInbound(MessageType.TRANS, translated, in.getUserId(), in.getLang(),in.getRoomId());
    }

}