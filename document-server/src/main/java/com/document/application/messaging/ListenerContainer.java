package com.document.application.messaging;


import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ListenerContainer {

	public static final String DOCUMENT_STREAM_KEY = "summary-jobs";
	public static final String DOCUMENT_CONSUMER_GROUP = "summary-workers";

	@Value("${redis.stream.consumer-name}")
	private String consumerName;

	private final SummaryJobSubscriber summaryJobSubscriber;

	/*
	 * Redis Stream Consumer 설정
	 */
	@Bean
	public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer(
		RedisConnectionFactory connectionFactory,
		RedisTemplate<String, String> redisTemplate
	) {

		try {
			redisTemplate.opsForStream().createGroup(DOCUMENT_STREAM_KEY, DOCUMENT_CONSUMER_GROUP);
		} catch (Exception e) {
			log.info("Consumer Group '{}' 이미 존재함, 생성 무시", DOCUMENT_CONSUMER_GROUP);
		}

		StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
			StreamMessageListenerContainer.StreamMessageListenerContainerOptions
				.builder()
				.pollTimeout(Duration.ofSeconds(1))
				.build();

		StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
			StreamMessageListenerContainer.create(connectionFactory, options);

		container.receive(
			Consumer.from(DOCUMENT_CONSUMER_GROUP, consumerName),
			StreamOffset.create(DOCUMENT_STREAM_KEY, ReadOffset.lastConsumed()),
			summaryJobSubscriber
		);

		container.start();

		return container;
	}
}
