package com.chat.llm;

import com.chat.llm.model.CompleteAnswer;
import com.chat.llm.model.Delta;
import com.chat.llm.model.Usage;
import com.chat.rag.model.SearchPlan;
import jakarta.annotation.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LlmClient {
    //1차: 질의 -> 검색 계획 생성(쿼리 확장/키워드)

    /**
     * 첫 번째 LLM 호출
     * -> 사용자의 원본 텍스트를 받아 검색 엔진에 맞게 재작성하는 것이 목적
     * @param userTextKo : 사용자의 원본 질문(한국어)
     * @param traceId : 단일 사용자 요청이 여러 서비스(예: 채팅 서버, LLM 서비스, 검색 서비스) 를 거쳐 흘러갈 때 추적할 수 있게 해주는 식별자
     * @return Mono<SearchPlan>: 이 메서드가 비동기적으로 동작하며, 최종적으로 하나의 SearchPlan 객체를 반환할 것임을 나타냄
     * 역할: 이 메서드의 역할은 질문에 직접 답하는 것이 아니라, 어떻게 하면 답변을 가장 잘 찾을 수 있을지 생각하는 것
     * -> 사용자의 모호한 쿼리를 분해하여 Vertex Ai Search에서 최상의 결과를 낼 수 있는, 기계가 읽을 수 있는 정밀한 지침(SearchPlan)으로 변환
     */
    Mono<SearchPlan> rewriteForSearch(String userTextKo, String traceId);

    /**
     * 두 번째이자 마지막 LLM 호출
     * @param prompt 사용자의 원본 질문에 더해 Vertex Ai Search에서 검색된 모든 컨텍스트(문맥 정보)를 포함하는 포괄적인 프롬프트
     *               -> LLM은 이 풍부한 컨텍스트를 사용하여 사실에 기반한 정확한 응답을 생성
     * @param traceid :첫 번쨰 호출과 동일한 traceId 사용
     * @return Flux<Delta> 실시간으로 GPT처럼 답변이 타이핑되는 듯한 경험을 만드는 핵심
     * -> 전체 답변을 한 번에 기다리는 대신, 이 메서드는 시간의 흐름에 따라 여러 Delta 객체로 이루어진 스트림을 반환
     * -> 각 Delta는 최종 답변의 작은 조각을 담고 있다.
     */
    //Flux<Delta> streamAnswer(String systemPrompt, String userPrompt, String traceId);

    public Mono<CompleteAnswer> getCompleteAnswer(String systemPrompt, String userPrompt, String traceId);
    @Nullable
    Usage lastUsage(String traceId);


}
