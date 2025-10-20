package com.document.application.messaging;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JobCreatedEvent 리스너
 * 트랜잭션 커밋 후에 Redis Stream에 메시지를 발행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobCreatedEventListener {

	private final SummaryJobPublisher summaryJobPublisher;

	/**
	 * 트랜잭션 커밋 후에 실행됩니다.
	 * 이를 통해 DB에 Job이 커밋된 후 Redis에 메시지를 보내서
	 * Consumer가 DB 조회 시 Job을 찾을 수 있도록 보장합니다.
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleJobCreatedEvent(JobCreatedEvent event) {
		log.info("트랜잭션 커밋 완료 - Redis Stream에 메시지 발행: jobId={}", event.getJobId());
		summaryJobPublisher.publishJobCreated(event.getJobId());
	}
}
