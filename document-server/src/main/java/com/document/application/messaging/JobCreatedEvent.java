package com.document.application.messaging;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * SummaryJob 생성 이벤트
 * 트랜잭션 커밋 후 Redis Stream에 메시지를 발행하기 위한 이벤트
 */
@Getter
@RequiredArgsConstructor
public class JobCreatedEvent {
	private final Long jobId;
}
