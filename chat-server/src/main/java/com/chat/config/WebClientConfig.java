package com.chat.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;


/**
 * 애플리케이션 시작 -> @Configuration 클래스 읽음 -> 그 안의 @Bean 메서드들  스캔
 * -> sttWebClient() 호출 -> WebClient 객체 만들어짐 -> 컨테이너에 sttWebClient라는 이름으로 등록
 *
 *
 */
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    //application.yml의 값들을 AppProperties 클래스로 바인딩해서 넣음
    private final AppProperties props;

    /**
     * HttpClient (Reactor Netty의 저수준 논블로킹 클라이언트)
     * 역할: 실제 소켓/이벤트루프/커넥션풀/TLS/타임아웃 같은 네트워크 레벨을 다룬다.
     * -> 각종 타임아웃과 채널 핸들러를 심어 놓고, 그걸 WebClient가 쓰도록 어댑터(ReactorClientHttpConnector)로 감싸서 반환
     *-> sttWebClient()가 내부에서 쓰는 HttpClient에 옵션이 결려있으면, 그 WebClient가 만드는 모든 신규 소켓 연결에 동일하게 적용
     *
     *코드 분석
     * .option(...)
     * TCP 연결 수립 단계에서의 최대 대기시간을 설정
     */
    private ReactorClientHttpConnector connector(int timeoutMs) {
        HttpClient http = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMs)
                .responseTimeout(Duration.ofMillis(timeoutMs))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(timeoutMs, TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(timeoutMs, TimeUnit.MILLISECONDS)));
        return new ReactorClientHttpConnector(http);
    }

    @Bean
    public WebClient sttWebClient(){
        return WebClient.builder()
                .baseUrl(props.getStt().getBaseUrl())
                .clientConnector(connector(props.getStt().getConnectTimeoutMs()))
                .build();
    }
/*
    @Bean
    public WebClient llmWebClient() {
        return WebClient.builder()
                .clientConnector(connector(props.getLlm().getTimeoutMs()))
                .baseUrl(props.getLlm().getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + props.getLlm().getApiKey())
                .build();
    }

    @Bean
    public WebClient ttsWebClient() {
        return WebClient.builder()
                .clientConnector(connector(props.getTts().getTimeoutMs()))
                .baseUrl(props.getTts().getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + props.getTts().getApiKey())
                .build();
    }
    */

}
