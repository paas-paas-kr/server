package com.document.application.messaging;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.common.exception.document.DocumentErrorCode;
import com.common.exception.document.DocumentException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SummaryJobPublisher {

	private final RedisTemplate<String, String> redisTemplate;

	private static final long STREAM_MAXLEN = 10_000L; // 필요 시 조정

	public void publishJobCreated(final Long jobId) {
		try {
			Map<String, String> body = new HashMap<>();
			body.put("jobId", String.valueOf(jobId));
			body.put("timestamp", String.valueOf(System.currentTimeMillis()));

			MapRecord<String, String, String> record = StreamRecords
				.mapBacked(body)
				.withStreamKey(ListenerContainer.DOCUMENT_STREAM_KEY);

			RedisStreamCommands.XAddOptions options = RedisStreamCommands.XAddOptions.maxlen(STREAM_MAXLEN)
				.approximateTrimming(true);

			redisTemplate.opsForStream().add(record, options);

		} catch (Exception e) {
			throw DocumentException.from(DocumentErrorCode.DOCUMENT_PROCESSING_ERROR);
		}
	}
}
