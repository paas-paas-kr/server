package com.chat.rag.model;


/**
 * Vertex Ai Search를 통해 검색된 개별 정보의 출처(인용 정보)를 담은 객체
 * RAG 시스템에서 LLM이 생성한 답변의 신뢰도를 높이기 위해 "이 내용은 여기에서 가져왔습니다"라고 근거를 제시할 때 사용
 * @param id: 검색 결과의 고유 식별자
 * @param title : 출처 문서 또는 웹페이지의 제목
 * @param url : 해당 출처로 바로 이동할 수 있는 웹 주소
 * @param snippet : 검색엔진이 보여주는 것처럼, 문서 내용 중 검색어와 가장 관련 높은 부분을 짧게 요약한 내용
 * 핵심 역할 및 사용 시나리오: LLM이 "다문화가정 장학금으로는 A와 B가 있습니다."라고 답변할 때, 답변 하단에 이 Citation 객체들을 리스트 형태로 보여준다.
 * 사용자는 제목과 요약(snippet)을 보고, URL을 클릭하여 직접 원본 정보를 확인할 수 있다.
 */
public record Citation(String id, String title, String url, String snippet) {
}
