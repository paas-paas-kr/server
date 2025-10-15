package com.document.application.cache;

import com.document.domain.enumtype.JobStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis Hash를 사용하여 작업 진행 상황을 실시간으로 캐싱합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobStatusCache {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String KEY_PREFIX = "job:status:";
    private static final Duration TTL = Duration.ofHours(24); // 24시간 후 자동 삭제

    /**
     * 작업 상태를 Redis Hash에 저장합니다.
     */
    public void updateStatus(final Long jobId, final JobStatus status) {
        String key = KEY_PREFIX + jobId;

        Map<String, String> data = new HashMap<>();
        data.put("jobId", jobId.toString());
        data.put("status", status.name());
        data.put("timestamp", String.valueOf(System.currentTimeMillis()));

        redisTemplate.opsForHash().putAll(key, data);
        redisTemplate.expire(key, TTL);

    }

    /**
     * 작업 상태를 Redis Hash에서 조회합니다.
     */
    public Map<String, String> getStatus(Long jobId) {
        String key = KEY_PREFIX + jobId;
        Map<Object, Object> rawData = redisTemplate.opsForHash().entries(key);

        if (rawData.isEmpty()) {
            return null;
        }

        // Object -> String 변환
        Map<String, String> data = new HashMap<>();
        rawData.forEach((k, v) -> data.put(k.toString(), v.toString()));

        return data;
    }

    /**
     * 작업 캐시를 삭제합니다.
     */
    public void delete(Long jobId) {
        String key = KEY_PREFIX + jobId;
        redisTemplate.delete(key);
    }
}
