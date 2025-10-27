package com.chat.rag;

import com.chat.config.AppProperties;
import com.chat.rag.model.Citation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Vertex AI Search API와 통신하는 SearchClient 구현체.
 * WebClient를 사용하여 비동기 논블로킹 방식으로 검색을 수행한다.
 */
@Slf4j // Lombok: SLF4J 로거를 자동으로 주입한다.
@Service // Spring: 이 클래스를 서비스 빈으로 등록한다.
@RequiredArgsConstructor // Lombok: final 필드에 대한 생성자를 자동으로 주입한다.
public class VertexSearchClient implements SearchClient {

    // Vertex AI와 통신하기 위한 비동기 WebClient (Bean으로 주입받음)
    private final WebClient vertexWebClient;
    // application.yml 등에서 Vertex 관련 설정(프로젝트 ID 등)을 가져온다.
    private final AppProperties props;

    /**
     * Vertex AI Search에 검색 쿼리를 비동기적으로 전송하고,
     * 검색 결과(Citation 리스트)를 Mono로 반환한다.
     *
     * @param query 검색할 질의어
     * @param topK  가져올 최대 결과 개수
     * @return 검색 결과 Citation 리스트를 담은 Mono
     */
    @Override
    public Mono<List<Citation>> search(String query, int topK) {
        // 1. API 경로(path) 동적 구성
        String path = String.format(
                "/v1/projects/%s/locations/%s/collections/default_collection/dataStores/%s/servingConfigs/default_search:search",
                props.getVertex().getProjectId(), props.getVertex().getLocation(), props.getVertex().getDataStoreId());

        // 2. topK 값 보정 (1 ~ 50 사이)
        int k = Math.max(1, Math.min(topK, 50));

        // 3. API 요청 본문(body) 생성. Map.of()는 변경 불가능한 Map을 만든다.
        Map<String, Object> body = Map.of("query", query, "pageSize", k); // topK 대신 k 사용 권장

        System.out.println("search 시작"); // 디버깅 로그

        // 4. WebClient를 사용한 비동기 POST 요청 파이프라인 시작
        return vertexWebClient.post()
                .uri(uriBuilder -> uriBuilder.path(path).build()) // 1번에서 만든 경로 설정
                .contentType(MediaType.APPLICATION_JSON) // 요청 본문은 JSON
                .bodyValue(body) // 3번에서 만든 본문 설정
                .retrieve() // 응답 수신 시작
                // 5. 비정상 응답(에러) 처리
                .onStatus(HttpStatusCode::isError, r -> r.bodyToMono(String.class) // 에러 응답 본문을 문자열로 읽음
                        .defaultIfEmpty("") // 에러 본문이 비어있을 경우 빈 문자열로 대체
                        .flatMap(b -> {
                            // 에러 로그 남기기
                            log.warn("[VertexSearch] status={} body={}", r.statusCode(), b);
                            // 에러 신호(Mono.error)를 발생시켜 파이프라인 중단
                            return Mono.error(new RuntimeException("VertexSearch error: " + r.statusCode()));
                        }))
                // 6. 정상 응답 본문을 Map.class로 변환
                .bodyToMono(Map.class)
                // 7. (부수 효과) 파이프라인이 "구독"될 때(실행 시작 시) 로그
                .doOnSubscribe(s -> log.info("[VertexSearch] subscribed"))   // ✅ 여기!
                // 8. (부수 효과) Map 데이터를 "받았을" 때 로그 (데이터 훔쳐보기)
                .doOnNext(map -> log.info("[VertexSearch] got keys={}", map.keySet()))
                // 9. (부수 효과) 파이프라인 "에러" 발생 시 로그
                .doOnError(e -> log.error("[VertexSearch] error", e))
                // 10. (부수 효과) 파이프라인이 "종료"될 때(성공/실패/취소 무관) 로그
                .doFinally(sig -> log.info("[VertexSearch] done: {}", sig))
                // 11. 타임아웃 설정: 5초 이상 응답 없으면 에러 발생
                .timeout(Duration.ofSeconds(5))
                // 12. [map 동기] 최종적으로 응답 Map을 Citation 리스트로 변환
                .map(this::toCitations);
    }

    /**
     * Vertex AI Search API의 응답(Map)을 파싱하여
     * Citation 객체 리스트로 변환하는 헬퍼 메서드.
     *
     * @param res API가 반환한 Map
     * @return Citation 리스트
     */
    @SuppressWarnings("unchecked") // 응답 Map에서 제네릭 캐스팅이 필요하므로 경고 억제
    private List<Citation> toCitations(Map<String, Object> res) {

        System.out.println("res" + res); // 디버깅 로그
        // 1. 응답의 최상위 "results" 배열을 꺼냄. 없으면 빈 리스트 반환
        var results = (List<Map<String, Object>>) res.getOrDefault("results", List.of());
        List<Citation> cites = new ArrayList<>(results.size());

        // 2. 각 result 항목을 순회
        for (var r : results) {
            // "document" 객체 추출
            Map<String, Object> doc = (Map<String, Object>) r.get("document");
            if (doc == null) {
                log.warn("[VertexSearch] Skipping result with missing 'document': {}", r);
                continue; // "document"가 없으면 이 결과는 건너뜀 (파싱 불가)
            }

            // "document" 내의 "derivedStructData" (메타데이터) 추출
            Map<String, Object> derived = (Map<String, Object>) doc.get("derivedStructData");
            if (derived == null) {
                log.warn("[VertexSearch] Skipping result with missing 'derivedStructData' in document: {}", doc);
                continue; // "derivedStructData"가 없으면 건너뜀 (파싱 불가)
            }

            // 3. 필요한 메타데이터(id, title, url) 추출
            String id = String.valueOf(doc.get("name")); // 문서 고유 ID
            String title = String.valueOf(derived.getOrDefault("title", "")); // "title"이 없으면 빈 문자열
            String url = String.valueOf(derived.getOrDefault("link", "")); // "link"가 없으면 빈 문자열

            String snip = ""; // 스니펫 기본값

            // 4. 스니펫(snippets) 안전하게 추출 (중첩 구조 + 타입 체크)
            // (경로: derived.snippets[0].snippet)

            // 4-1. "snippets" 키가 존재하고 List 타입인지 확인
            if (derived.get("snippets") instanceof List<?> snippetsList) {
                // 4-2. List가 비어있지 않고 첫 번째 요소가 Map 타입인지 확인
                if (!snippetsList.isEmpty() && snippetsList.get(0) instanceof Map<?, ?> firstSnippetMap) {
                    // 4-3. "snippet" 키의 값을 Object로 가져옴 (null일 수 있음)
                    Object snippetValue = firstSnippetMap.get("snippet");
                    // 4-4. 값이 null이 아닐 경우 String으로 변환
                    if (snippetValue != null) {
                        snip = String.valueOf(snippetValue);
                    }
                }
            }

            // 5. 스니펫 길이 제한
            if (snip.length() > 500) snip = snip.substring(0, 500) + "…";

            // 6. 유효한 데이터(url, title, snip 중 하나라도)가 있을 경우에만 리스트에 추가
            if (!url.isBlank() || !title.isBlank() || !snip.isBlank()) {
                cites.add(new Citation(id, title, url, snip));
            }
            // 디버깅 로그
            System.out.println("id: " + id);
            System.out.println("title: " + title);
            System.out.println("url: " + url);
            System.out.println("snip: " + snip);
        }

        // 7. 중복 URL 제거 (스트림 사용)
        // (가끔 Vertex Search가 동일 URL을 여러 번 반환할 수 있음)
        return cites.stream()
                .collect(java.util.stream.Collectors.collectingAndThen(
                        // 7-1. LinkedHashMap을 사용하여 순서를 보장하며 중복 키(URL)를 덮어씀
                        java.util.stream.Collectors.toMap(
                                // 7-2. 키: URL (URL이 비어있으면 ID를 대신 키로 사용)
                                c -> c.url().isBlank() ? c.id() : c.url(),
                                // 7-3. 값: Citation 객체 자체
                                c -> c,
                                // 7-4. 중복 키 발생 시: 기존 값(a)을 유지 (새 값(b) 버림)
                                (a, b) -> a,
                                // 7-5. 순서 유지를 위해 LinkedHashMap 사용
                                LinkedHashMap::new),
                        // 7-6. Map의 값(values)들만 뽑아서 새 ArrayList로 반환
                        m -> new ArrayList<>(m.values())
                ));
    }
}

/*
(참고용 Vertex AI Search 응답 JSON 구조 일부)
{
  "results": [
    {
      "id": string,
      "document": {
          "name": string, // (이걸 id로 사용)
          "derivedStructData": {
            "link": "...", // (이걸 url로 사용)
            "title": "...", // (이걸 title로 사용)
            "snippets": [ // (이걸 snip으로 사용)
              {
                "snippet": "..."
              }
            ]
          }
      }
    }
  ],
  "totalSize": integer,
  ...
}
 */