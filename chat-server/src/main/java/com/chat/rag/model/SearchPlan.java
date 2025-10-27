package com.chat.rag.model;

import java.util.List;
import java.util.Map;

/**
 * 역할: 이 레코드는 LLM이 사용자의 질문을 분석하여 Vertex AI Search를 어떻게 호출할지 계획한 내용을 담는 객체
 * 즉, LLM -> RAG -> LLM 아키텍처에서 첫 번째 LLM의 출력물
 * @param queries LLM이 사용자 질문을 하나 이상의 구체적인 검색어로 변환한 목록, 복잡한 질문일수록 여러 개의 검색어가 필요
 * @param filters 특정 조건을 적용하여 검색 결과를 필터링할 때 사용 (예: {"site": "go.kr", "filetype":"pdf"} -> 정부 사이트의 PDF 파일만 검색
 * 핵심 역할 및 사용 시나리오:
 * - 사용자가 "아이 국적 관련해서 알려줘"라고 모호하게 질문하면,
 * - 첫 번째 LLM이 이를 분석하여 SearchPlan.of(List.of("대한민국 국적법", "다문화가족 자녀 국적 취득")) 과 같은 구체적인 검색 계획을 생성
 * - 이 객체를 Vertex AI Search 클라이언트에 전달하여 검색을 수행
 */
public record SearchPlan(List<String> queries, Map<String, Object> filters) {
    public static SearchPlan of(List<String> q){ return new SearchPlan(q,Map.of());}
}
