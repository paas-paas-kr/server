package com.chat.llm.model;

/**
 * 역할: LLM이 답변을 생성할 때, 전체 문장을 한 번에 보내지 않고 실시간으로 단어/토큰 단위로 쪼개서 보내는 스트리밍 응답의 각 조각을 담는 객체
 * @param text : 현재 전송되는 텍스트 조각 (예: "대한민국", "국적은", "부모...")
 * @param isFinal : 이 Delta가 전체 응답의 마지막 조각인지 여부를 나타내는 플래그
 * 핵심 역할 및 시나리오:
 * ChatGPT처럼 답변을 타이핑하듯 보여주는 기능을 구현할 때 사용
 * 웹소켓을 통해 서버는 이 Delta 객체를 계속해서 클라이언트로 보내고, 클라이언트는 text를 이어 붙여 화면에 보여주다가
 * isFinal이 true인 Delta를 받으면 응답이 완료되었다고 판단
 */
public record Delta(String text, boolean isFinal) {
}
