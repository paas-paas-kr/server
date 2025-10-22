package com.document.application.messaging;

import static com.document.application.messaging.ListenerContainer.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import com.document.application.summary.DocumentSummarizer;

@Slf4j
@Component
@RequiredArgsConstructor
public class SummaryJobSubscriber implements StreamListener<String, MapRecord<String, String, String>> {

	private final RedisTemplate<String, String> redisTemplate;
	private final DocumentSummarizer documentSummarizer;

	/**
	 * Redis Stream에서 메시지를 수신합니다.
	 */
	@Override
	public void onMessage(MapRecord<String, String, String> message) {
		Long jobId = null;
		try {
			jobId = Long.parseLong(message.getValue().get("jobId"));
			documentSummarizer.processSummaryJob(jobId);

			// 처리 성공 시 ACK
			redisTemplate.opsForStream().acknowledge(DOCUMENT_CONSUMER_GROUP, message);
		} catch (Exception e) {
			log.error("Redis Stream 메시지 처리 실패 - jobId: {}, 오류: {}", jobId, e.getMessage(), e);
			if (jobId != null) {
				documentSummarizer.handleJobFailure(jobId);
			}
			// 실패한 경우에도 ACK를 보내서 다음 메시지를 처리할 수 있도록 함
			// DLQ(Dead Letter Queue)가 필요한 경우 여기서 별도 처리 가능
			redisTemplate.opsForStream().acknowledge(DOCUMENT_CONSUMER_GROUP, message);
		}
	}
}
