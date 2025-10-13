package com.chat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 전체 서비스 설정 클래스.
 * application.yml의 app.* 구조를 바인딩함.
 *
 * 예시:
 * app:
 *   stt:
 *     base-url: https://naveropenapi.apigw.ntruss.com
 *     path: /recog/v1/stt
 *     language: Kor
 *     connect-timeout-ms: 3000
 *     read-timeout-ms: 15000
 *     api-key-id: ${NCP_APIGW_API_KEY_ID}
 *     api-key: ${NCP_APIGW_API_KEY}
 *   audio:
 *     ffmpeg-path: /usr/bin/ffmpeg
 *     mock:
 *       enabled: false
 *       on: META
 *       sample: classpath:/mock/sample-tts.ogg
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Stt stt = new Stt();
    private Audio audio = new Audio();

    @Data
    public static class Stt {
        private String baseUrl;            // https://naveropenapi.apigw.ntruss.com
        private String path;               // /recog/v1/stt
        private String language = "Kor";

        // 개별 타임아웃
        private int connectTimeoutMs = 3000;
        private int readTimeoutMs = 15000;

        private String apiKeyId;
        private String apiKey;

        // ---------- 호환용 alias (getTimeoutMs 호출 대응) ----------
        /** 호출부에서 getTimeoutMs()를 기대하는 경우를 위해 추가 */
        public int getTimeoutMs() {
            // 관례상 readTimeout을 대표값으로 반환 (원하면 connect로 바꿔도 됨)
            return readTimeoutMs;
        }

        /** app.stt.timeout-ms 로 설정하면 두 타임아웃을 동시에 세팅 */
        public void setTimeoutMs(int ms) {
            this.connectTimeoutMs = ms;
            this.readTimeoutMs = ms;
        }
    }

    @Data
    public static class Audio {
        private String ffmpegPath = "/usr/bin/ffmpeg";

        private Mock mock = new Mock();

        @Data
        public static class Mock {
            private boolean enabled = false;
            private String on = "META"; // META | CHUNK | FINAL
            private String sample = "classpath:/mock/sample-tts.ogg";
        }
    }
}
