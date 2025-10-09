package com.chat.common.constants;

/**
 * WebSocket 통신에서 사용되는 메시지 타입을 상수로 정의한 클래스
 * 인스턴스화할 수 없도록 final과 private 생성자를 사용
 *
 * START: 클라이언트가 웹소켓에 처음 연결한 직후, 본격적인 데이터 교환을 시작하기 전에 보내는 신호
 * TEXT: 사용자가 실제로 보내는 핵심 데이터, 사용자가 입력한 질문 or 프롬포트가 여기에 담겨 서버로 전송
 * STOP: 서버가 긴 응답을 스트리밍으로 보내주고 있을 때, 클라이언트가 중간에 응답을 끊고 싶을 때 보내는 요청
 *
 * ACK: 클라이언트가 보낸 START 요청에 대한 확인 응답
 * PARTIAL: 생성되는 즉시 응답의 일부(partial)를 계속 보내주는 스트리밍 방식에 사용
 * FINAL: 여러 개의 PARTIAL 메시지로 답변을 모두 전송한 후, 답변 스트림은 끝났다는 메시지,
 * - 이 신호를 받은 클라이언트는 응답이 완료되었음을 인지하고 입력창을 다시 활성화하는 등의 후속 처리를 함
 * ERROR: 서버에서 LLM을 호출하다가 실패했거나, 요청 형식이 잘못되는 등 오류 상황
 */

public enum MessageType {
    START,
    CHAT,
    PING,
    AUDIO_CHUNK,
    STOP
}
