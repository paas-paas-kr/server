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

			redisTemplate.opsForStream().acknowledge(DOCUMENT_CONSUMER_GROUP, message);
		} catch (Exception e) {
			log.error("Redis Stream 메시지 처리 실패 - jobId: {}, 오류: {}", jobId, e.getMessage(), e);
			if (jobId != null) {
				documentSummarizer.handleJobFailure(jobId);
			}
		}
	}
}
