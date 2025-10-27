package com.chat.pipeline;

import com.chat.common.json.JsonUtils;
import com.chat.common.ws.WsEmitter;
import com.chat.llm.LlmClient;
import com.chat.llm.PromptBuilder;
import com.chat.llm.model.CompleteAnswer;
import com.chat.llm.model.Delta; // (사용되지 않는 import)
import com.chat.rag.SearchClient;
import com.chat.rag.model.Citation;
import com.chat.rag.model.SearchPlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG(Retrieval-Augmented Generation) 파이프라인을 총괄하는 오케스트레이터.
 * "LLM-First" 접근 방식을 사용함: LLM이 먼저 검색 쿼리를 생성(rewrite)하고,
 * 그 쿼리로 검색(search)한 뒤, 결과를 모아 LLM에게 최종 답변을 생성(answer)하도록 요청함.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmFirstRagOrchestrator {

    // LLM API 클라이언트 (쿼리 재작성, 최종 답변 생성 담당)
    private final LlmClient llm;
    // 검색 API 클라이언트 (Vertex AI Search 등)
    private final SearchClient search;

    /**
     * RAG 파이프라인 전체를 실행한다.
     *
     * @param userTextKo 사용자의 원본 한국어 질문
     * @param emitter    WebSocket 클라이언트와 통신하기 위한 Emitter
     * @return LLM의 최종 답변 문자열을 담은 Mono<String>
     */
    public Mono<String> run(String userTextKo, WsEmitter emitter) {
        // WebSocket 세션 ID를 추적 ID로 사용
        String traceId = emitter.getSessionId();

        // --- 1. 검색 계획 생성 (LLM 호출 1) ---
        // 사용자의 질문을 LLM이 검색하기 좋은 쿼리로 재작성(rewrite)하도록 요청함.
        Mono<SearchPlan> planMono = llm.rewriteForSearch(userTextKo, traceId)
                // 15초 타임아웃: 15초 내에 쿼리 생성이 안 되면 에러 발생
                .timeout(Duration.ofSeconds(15))
                // (부수 효과) 에러 발생 시 클라이언트에게 'rewrite timeout' 에러 전송
                .doOnError(e -> emitter.emitText(JsonUtils.toJson(Map.of(
                        "type","nlp-stream","event","error","data", Map.of("message","rewrite timeout"),"traceId",traceId))));

        // --- 2. 검색 실행 (Search API 호출) ---
        // 1번에서 생성된 '검색 계획(plan)'을 받아서 실제 검색을 실행함.
        Mono<List<Citation>> citesMono = planMono.flatMap(plan -> {
            List<String> qs = plan.queries();
            // 쿼리가 비어있으면(LLM 실패), 원본 텍스트로 대신 검색
            if (qs == null || qs.isEmpty()) qs = List.of(userTextKo);

            log.info("[TraceID: {}] Generated Search Queries: {}", traceId, qs);

            // 쿼리 중 최대 2개만 병렬로 실행
            return Flux.fromIterable(qs.stream().limit(2).toList())
                    // flatMap(..., 2): 2개의 검색(search.search)을 '병렬'로 동시 실행함.
                    .flatMap(q -> search.search(q, 5), /*병렬*/ 2)
                    // Flux<List<Citation>> -> Flux<Citation> (리스트를 개별 Citation으로 펼침)
                    .flatMapIterable(list -> list)
                    // Citation::url 기준으로 중복된 Citation 제거
                    .distinct(Citation::url)
                    // 최대 5개의 고유한 Citation만 가져옴
                    .take(5)
                    // Flux<Citation> -> Mono<List<Citation>> (다시 리스트로 수집)
                    .collectList()
                    // (부수 효과) 수집된 최종 Citation 로깅
                    .doOnNext(c -> {
                        log.info("[TraceID: {}] Collected Citations (Size: {}): {}", traceId,c.size(), c);});
        });

        // --- 3. 최종 답변 생성 (LLM 호출 2) ---
        // 2번에서 수집된 '검색 결과(cites)'와 '원본 질문'을 조합해 프롬프트를 만듦.
        Mono<CompleteAnswer> answerMono = citesMono.flatMap(cites -> {
            log.info("[TraceID: {}] Citations passed to LLM Prompt (Size: {}): {}", traceId, cites.size(), cites);

            // 1. 시스템 프롬프트(역할)와 사용자 프롬프트(질문+검색결과)를 조립
            String systemPrompt = PromptBuilder.getSystemInstruction();
            String userPrompt = PromptBuilder.getUserContextPrompt(userTextKo, cites);

//            // (부수 효과) 클라이언트에게 "이제 LLM 답변 생성 시작"이라고 알림
//            emitter.emitText(JsonUtils.toJson(Map.of(
//                    "type","nlp-stream","event","progress","data", Map.of("stage","gen","detail","stream"),"traceId",traceId)));

            // 2. LLM에게 최종 답변(비스트리밍)을 요청
            return llm.getCompleteAnswer(systemPrompt, userPrompt, traceId);
        });

        // --- 4. 파이프라인 총괄 및 반환 ---
        // 1 -> 2 -> 3번 Mono를 순차적으로 실행하고,
        // 클라이언트에게 진행 상황/결과/에러를 전송(부수 효과)한 뒤,
        // 최종 LLM 답변(String)을 반환함.
        return answerMono
                // (부수 효과) 이 파이프라인 *전체*가 구독(시작)될 때 "rewrite 시작" 알림
//                .doOnSubscribe(s -> emitter.emitText(JsonUtils.toJson(Map.of(
//                        "type","nlp-stream","event","progress","data", Map.of("stage","rewrite","detail","begin"),"traceId",traceId))))
                // (부수 효과) LLM의 최종 답변(CompleteAnswer 객체) 로깅
                .doOnNext(ca -> System.out.println("CompleteAnswer.text ="+ ca.text()))
                // [map 동기] Mono<CompleteAnswer> -> Mono<String> (알맹이 변환)
                // 이 파이프라인의 최종 성공 결과는 LLM의 '텍스트' 문자열임.
                .map(CompleteAnswer::text)
                // 65초 타임아웃: (rewrite 15초 포함) 전체 RAG 파이프라인 시간 제한
                .timeout(Duration.ofSeconds(65))

                // --- 5. 최종 이벤트 처리 (성공/실패/완료) ---

                // (부수 효과) 파이프라인 *전체* 중 에러 발생 시
                .doOnError(e -> {
                    // 클라이언트에게 "error" 이벤트 전송
                    emitter.emitText(JsonUtils.toJson(Map.of(
                            "type","nlp-stream","event","error","data", Map.of("message", e.getMessage()),"traceId",traceId)));
                    // 에러는 상위(WebSocket 핸들러)로 계속 전파됨
                })
                // (부수 효과) 파이프라인 *전체*가 *어쨌든* 종료될 때 (성공/에러/취소)
                .doFinally(sig -> {
                    // "성공적으로 완료(ON_COMPLETE)"되었을 때만
                    if (sig == SignalType.ON_COMPLETE) {
                        // 1. 토큰 사용량(Usage) 로그
                        var u = llm.lastUsage(traceId);
                        if (u != null) {
                            log.info("usage promptTokens={} completionTokens={} totalTokens={}",
                                    u.promptTokens(), u.completionTokens(), u.totalTokens());
//                            emitter.emitText(JsonUtils.toJson(Map.of(
//                                    "type","nlp-stream","event","usage","data",
//                                    Map.of("promptTokens",u.promptTokens(),"completionTokens",u.completionTokens(),"totalTokens",u.totalTokens()),
//                                    "traceId",traceId)));
                        }
                        // 2. 최종 "done" 이벤트 로그
                        log.info("done finish=true");
//                        emitter.emitText(JsonUtils.toJson(Map.of(
//                                "type","nlp-stream","event","done","data", Map.of("finish", true),"traceId",traceId)));
                    }
                    // (참고: 에러(ON_ERROR)시에는 doOnError가 이미 처리했으므로 여기선 'done' 안 보냄)
                });
    }
}