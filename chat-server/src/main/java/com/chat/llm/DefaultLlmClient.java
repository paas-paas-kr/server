package com.chat.llm;

import com.chat.config.AppProperties;
import com.chat.llm.model.CompleteAnswer;
import com.chat.llm.model.Usage;
import com.chat.rag.model.SearchPlan;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * LlmClient 인터페이스의 기본 구현체.
 * Google Gemini API와 비동기 통신(WebClient)을 수행한다.
 */
@Service
@RequiredArgsConstructor // Lombok: final 필드에 대한 생성자를 자동으로 주입한다.
public class DefaultLlmClient implements LlmClient {

    // 비동기 HTTP 통신을 위한 Spring WebClient (Bean으로 주입받음)
    private final WebClient llmWebClient;
    // traceId별 토큰 사용량을 저장하기 위한 동시성 지원 맵
    // (WebSocket 세션별로 토큰 사용량을 추적하기 위함)
    private final ConcurrentMap<String, Usage> usageMap = new ConcurrentHashMap<>();
    // 애플리케이션 설정(API 키 등)을 주입받는다.
    private final AppProperties props;

    /**
     * 사용자의 자연어 질문(한국어)을 받아, Vertex AI Search에 최적화된
     * 검색 쿼리(SearchPlan)를 비동기적으로 반환한다.
     * Gemini의 JSON 모드를 사용하여 안정적인 JSON 응답을 강제한다.
     *
     * @param userTextKo 한국어 원문 질문
     * @param traceId    요청 추적 ID
     * @return 검색 쿼리가 담긴 Mono<SearchPlan>
     */
    @Override
    public Mono<SearchPlan> rewriteForSearch(String userTextKo, String traceId) {

        // 1. Gemini API에 전송할 HTTP Body(Map) 생성
        Map<String, Object> body = Map.of(
                // 1) 시스템 프롬프트: AI의 역할(쿼리 재작성)과 출력 형식을 강력하게 지시
                "system_instruction", Map.of(
                        "parts", List.of(Map.of("text",
                                """
                                        [SYSTEM ROLE]
                                        너는 대한민국 다문화 가정을 위한 정보 검색 시스템의 **쿼리 재작성 전문 AI**이다.
                                        너의 **유일한 임무**는 사용자의 자연어 질문을 분석하여 Vertex AI Search 엔진이 관련 문서를 가장 효과적으로 찾을 수 있도록 **최적의 검색 쿼리 2개**를 생성하는 것이다.
                                        검색 대상 DB는 다문화 지원 포털('다누리', '한울타리'), 지역 센터 웹사이트, 정부 정책 문서 등이다.
                                        
                                        [OUTPUT REQUIREMENT - CRITICAL]
                                        -   **절대적으로 JSON 형식만 출력해야 한다.** 응답은 반드시 `{"queries": ["쿼리1", "쿼리2", "쿼리3"]}` 형태여야 한다.
                                        -   **어떠한 추가 텍스트도 절대 포함해서는 안 된다.** (예: "다음은 생성된 쿼리입니다:", 설명, 사과, 인사말, 코드 블록 마크다운(` ```json ... ``` `) 등 모두 금지)
                                        -   **오직 JSON 객체 하나만** 응답의 시작부터 끝까지 존재해야 한다.
                                        
                                        [QUERY GENERATION GUIDELINES]
                                        -   사용자 질문의 핵심 **키워드**와 **의도**를 정확히 파악하라.
                                        -   생성할 쿼리는 **2개**로 고정한다.
                                        -   쿼리는 **간결한 명사형 키워드 조합**을 사용해야 한다. (예: "다문화 자녀 학교 적응 지원", "결혼이민자 취업 비자 변경", "한국어 교육 무료 강좌")
                                        -   **절대로 완전한 문장, 질문, 서술형 표현을 사용하지 마라.** (나쁜 예: "다문화 자녀가 어떻게 학교에 적응할 수 있나요?", "결혼 이민자를 위한 취업 지원 프로그램을 알려주세요.")
                                        -   쿼리는 Vertex AI Search가 이해하기 쉬운 **검색 엔진 친화적인 형태**여야 한다.
                                        
                                        [FINAL WARNING]
                                        **JSON 형식 및 내용 요구사항을 정확히 따르지 않으면 결과는 실패로 간주된다. 다른 모든 텍스트 없이 오직 지정된 JSON 형식의 쿼리 3개만 생성하라.**
                                        """
                        ))
                ),
                // 2) 사용자 입력: "원문 질의: " 프리픽스를 붙여 LLM이 질문임을 명확히 인지하게 한다.
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", "원문 질의: " + userTextKo))
                )),
                // 3) 생성 설정: JSON 모드 및 스키마 정의
                "generationConfig", Map.of(
                        "temperature", 0.0, // 창의성을 0으로 설정하여 일관된 쿼리 생성
                        "topP", 1.0,
                        "maxOutputTokens", 1024,
                        // ★★★ 핵심: Gemini가 JSON으로만 응답하도록 강제
                        "responseMimeType", "application/json",
                        // ★★★ 핵심: Gemini가 따라야 할 JSON 스키마를 명시
                        "responseSchema", Map.of(
                                "type", "OBJECT",
                                "properties", Map.of(
                                        "queries", Map.of(
                                                "type", "ARRAY",
                                                "items", Map.of("type", "STRING"),
                                                "minItems", 2, // 최소 2개
                                                "maxItems", 4  // 최대 4개 (프롬프트의 2개 고정 지시와 약간 다름, 유연성)
                                        )
                                ),
                                "required", List.of("queries") // 'queries' 필드 필수
                        )
                )
        );

        // 4) WebClient로 API 비동기 호출
        return llmWebClient.post()
                .uri("/v1beta/models/gemini-2.5-flash:generateContent") // API 엔드포인트
                .header("x-goog-api-key", props.getLlm().getApiKey()) // API 키 설정
                .contentType(MediaType.APPLICATION_JSON) // 요청 본문은 JSON
                .bodyValue(body) // 위에서 생성한 body 객체
                .retrieve() // 응답 수신 시작
                // 5) 에러 처리: 4xx(클라이언트) 또는 5xx(서버) 에러 발생 시
                .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(), resp ->
                        // 에러 응답 본문(String)을 읽어 Mono<Error>로 변환
                        resp.bodyToMono(String.class).flatMap(err ->
                                Mono.error(new IllegalArgumentException("Gemini API error: " + err))
                        )
                )
                // 6) 응답 본문(Map) 받기 (Gemini의 'candidates' 래퍼 구조)
                .bodyToMono(Map.class)
                // 7) [map 동기] 중첩된 응답 Map에서 실제 텍스트(JSON 문자열) 추출
                .map(this::extractTextFromCandidates)
                // 8) [map 동기] 추출된 텍스트(JSON 문자열)를 다시 Map<String, Object>로 파싱
                .map(this::parseJsonToMapSafe)
                // 9) [map 동기] 파싱된 Map에서 "queries" 리스트를 꺼내 SearchPlan 객체 생성
                .map(res -> {
                    // getOrDefault로 'queries' 키가 없거나 null일 때 폴백 리스트 사용
                    var q = (List<String>) res.getOrDefault("queries", List.of(userTextKo));
                    // 리스트가 null이거나 비어있을 경우, 폴백으로 원본 텍스트 사용
                    var queries = (q == null || q.isEmpty()) ? List.of(userTextKo) : q;
                    System.out.println("123" + queries); // 디버깅 로깅
                    return SearchPlan.of(queries);
                })
                // 10) 폴백: 파이프라인 중 어디선가 (API 호출, 파싱 등) 에러가 나면
                //      최소한 원본 텍스트로라도 검색하도록 SearchPlan 반환 (최종 방어)
                .onErrorReturn(SearchPlan.of(List.of(userTextKo)));
    }


    /**
     * 시스템 프롬프트와 사용자 프롬프트를 받아, LLM의 완전한 답변(CompleteAnswer)을
     * 비동기적으로 반환한다. 토큰 사용량(Usage)을 함께 파싱한다.
     * (이 메서드는 JSON 모드를 사용하지 않음)
     *
     * @param systemPrompt 시스템 프롬프트 (AI의 역할)
     * @param userPrompt   사용자 프롬프트 (질문)
     * @param traceId      요청 추적 ID
     * @return 답변 텍스트와 토큰 사용량이 담긴 Mono<CompleteAnswer>
     */
    @Override
    public Mono<CompleteAnswer> getCompleteAnswer(String systemPrompt, String userPrompt, String traceId) {

        // 1. API Body 생성 (시스템 프롬프트와 사용자 입력)
        var systemInstruction = Map.of(
                "parts", List.of(Map.of("text", systemPrompt))
        );
        var userContents = List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userPrompt))
        ));
        Map<String, Object> body = Map.of(
                "system_instruction", systemInstruction,
                "contents", userContents
        );

        // 2. WebClient로 API 비동기 호출 (일반 모드)
        return llmWebClient.post()
                .uri("/v1beta/models/gemini-2.5-flash:generateContent")
                .header("x-goog-api-key", props.getLlm().getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON) // JSON 응답 선호
                .bodyValue(body)
                .retrieve()
                // 3. 에러 처리
                .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(), resp ->
                        resp.bodyToMono(String.class).flatMap(err ->
                                Mono.error(new RuntimeException("Gemini API Error: " + err))
                        )
                )
                // 4. 응답 본문을 제네릭 Map<String, Object> 타입으로 받음
                // (Map.class 대신 ParameterizedTypeReference를 사용해야 제네릭 타입 추론 가능)
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                // 5. [map 동기] 응답 Map을 파싱하여 CompleteAnswer 객체로 변환
                .map(data -> {
                    try {
                        System.out.println(data); // 디버깅용 원본 응답 로깅

                        // 5a. 텍스트 추출 (candidates[0].content.parts[0].text)
                        // (NPE 발생 가능성이 있는 위험한 코드. try-catch로 감싸져 있음)
                        List<Map<String, Object>> candidates = (List<Map<String, Object>>) data.get("candidates");
                        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                        String text = (String) parts.get(0).get("text");

                        // 5b. 토큰 사용량(Usage) 추출 (usageMetadata)
                        Map<String, Object> usageData = (Map<String, Object>) data.get("usageMetadata");
                        Usage u = new Usage(
                                // getOrDefault와 Number 캐스팅으로 NPE(NullPointerException) 및 타입 오류 방지
                                ((Number) usageData.getOrDefault("promptTokenCount", 0)).intValue(),
                                ((Number) usageData.getOrDefault("candidatesTokenCount", 0)).intValue(),
                                ((Number) usageData.getOrDefault("totalTokenCount", 0)).intValue()
                        );

                        // 5c. 토큰 사용량 맵에 저장 (추후 조회를 위해)
                        usageMap.put(traceId, u);

                        System.out.println("FULL TEXT: " + text); // 디버깅용 최종 텍스트 로깅

                        // 5d. 최종 객체 반환
                        return new CompleteAnswer(text, u);

                    } catch (Exception e) {
                        // 5e. 파싱 실패 시: JSON 구조가 예상과 다르거나(NPE), 캐스팅 실패 시
                        System.err.println("!!! Data Casting Error: " + data + " | Error: " + e.getMessage());
                        // 에러를 전파하여 상위 스트림(onErrorReturn 등)에서 처리하도록 함
                        throw new RuntimeException("Failed to parse Gemini response", e);
                    }
                });
        // --------------------------
    }


    /**
     * Gemini 응답(Map)에서 `candidates[0].content.parts[0].text` 경로의 텍스트를
     * 안전하게 추출하는 헬퍼 메서드.
     *
     * @param resp WebClient가 반환한 Map
     * @return 추출된 텍스트, 없으면 "{}"
     */
    @SuppressWarnings("unchecked") // 응답 Map에서 제네릭 캐스팅(List<Map...>)이 필요하므로 경고 억제
    private String extractTextFromCandidates(Map<?, ?> resp) {
        System.out.println("1"); // 디버깅 로깅
        System.out.println(resp); // 디버깅 로깅

        // null 검사를 연쇄적으로 수행하여 NPE(NullPointerException)를 방지한다.
        var candidates = (List<Map<String, Object>>) resp.get("candidates");
        if (candidates == null || candidates.isEmpty()) return "{}";

        var content = (Map<String, Object>) candidates.get(0).get("content");
        var parts = (List<Map<String, Object>>) (content != null ? content.get("parts") : null);
        if (parts == null || parts.isEmpty()) return "{}";

        // Gemini가 'parts'를 여러 개로 쪼개 보낼 경우를 대비해, 모든 'text'를 하나로 합친다.
        return parts.stream()
                .map(p -> (String) p.getOrDefault("text", ""))
                .reduce("", String::concat)
                .trim();
    }

    /**
     * LLM이 반환한 텍스트(JSON 문자열)를 Map<String, Object>로 파싱한다.
     * LLM이 JSON 외에 잡음(예: ```json ... ```)을 섞었을 경우를 대비해 폴백 로직을 포함한다.
     *
     * @param json LLM이 반환한 텍스트 (JSON 형식이어야 함)
     * @return 파싱된 Map. 실패 시 'queries'가 비어있는 Map 반환
     */
    private Map<String, Object> parseJsonToMapSafe(String json) {
        // 새 ObjectMapper 인스턴스 생성 (Thread-safe)
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        try {
            // 1차 시도: JSON 문자열을 Map으로 파싱
            return mapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            // 2차 시도 (폴백): 파싱 실패 시 (LLM이 JSON 외에 잡음을 섞었을 경우 대비)
            int s = json.indexOf('{'); // 가장 처음 '{'
            int e2 = json.lastIndexOf('}'); // 가장 마지막 '}'
            if (s >= 0 && e2 > s) {
                // 문자열에서 가장 바깥쪽 '{'와 '}'를 찾아 강제로 추출
                var maybe = json.substring(s, e2 + 1);
                try {
                    // 추출한 문자열로 다시 파싱 시도
                    return mapper.readValue(maybe, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                    });
                } catch (Exception ignore) {
                    // 2차 시도도 실패하면 무시
                }
            }
            // 최종 폴백: 2차 시도도 실패하면, 비어있는 'queries' 맵을 반환하여
            // 파이프라인이 중단되지 않게 한다. (onErrorReturn에서 원본 텍스트로 처리됨)
            return Map.of("queries", List.of());
        }
    }


    /**
     * 지정된 traceId(세션 ID)에 대해 `getCompleteAnswer`에서 저장했던
     * 마지막 토큰 사용량(Usage)을 반환한다.
     *
     * @param traceId 조회할 traceId
     * @return Usage 객체. 없으면 null
     */
    @Override
    public Usage lastUsage(String traceId) {
        return usageMap.get(traceId);
    }
}