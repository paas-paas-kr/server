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
        private String baseUrl;            // https://naveropenapi.apigw.ntruss.com/recog/v1
        private String path;               // /recog/v1/stt
        private String language = "Kor";

        // 서버와 TCP 연결을 맺을 때까지 기다리는 최대 시간
        /// 이 시간 안에 소켓 연결이 안 되면 ConnectTimeoutException으로 실패
        private int connectTimeoutMs = 3000;
        // 연결이 성사되고 요청을 보낸 뒤, 수신 무활동 시간의 최대치
        // 즉, 응답 바이트가 일정 시간 동안 전혀 들어오지 않으면 타임아웃으로 끊음
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
        private String ffmpegPath;

        private Mock mock = new Mock();

        @Data
        public static class Mock {
            //mock.enabled == true면 실제 stt/tts 호출 x
            private boolean enabled = false;
            //모킹을 언제 발사할지 트리거를 고르는 것
            // META: 클라이언트가 메타 데이터를 보내자마자 샘플 1건 전송
            // CHUNK: 오디오 청크를 받았을 때 샘플 전송
            // FINAL: 클라이언트가 {"type":"FINISH"}를 보냈을 때 샘플 전송
            private String on = "META"; // META | CHUNK | FINAL
            private String sample = "classpath:/mock/sample-tts.ogg";
        }
    }

}
