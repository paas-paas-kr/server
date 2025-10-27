package com.chat.llm.model;

/**
 * 역할: LLM API를 한 번 호출했을 때 소모된 토큰의 양을 기록하는 객체
 * -> API 비용을 계산하거나 사용량을 모니터링하기 위한 필수적인 정보
 * @param promptTokens : LLM에 입력으로 제공된 텍스트(질문+ 검색 컨텍스트)의 토큰 수
 * @param completionTokens : LLM이 생성한 답변 텍스트의 토큰 수
 * @param totalTokens : promptTokens와 completionTokens를 합한 총 토큰 수-> 이 값을 기준으로 비용이 청구
 * 핵심 역할 및 사용 시나리오: LLM API 응답에 보통 이 Usage 정보가 포함되어 온다.
 * 이 값을 데이터베이스에 기록해두면, 나중에 어떤 질문이 비용을 많이 발생시켰는지 분석하거나 사용자별 API 사용량을 제한하는 등의 기능을 구현할 수 있다.
 */
public record Usage(int promptTokens, int completionTokens, int totalTokens) {
}
