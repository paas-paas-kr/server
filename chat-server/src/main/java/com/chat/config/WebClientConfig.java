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

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {
    private final AppProperties props;

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
    public WebClient sttWebClient() {
        return WebClient.builder()
                .clientConnector(connector(props.getStt().getTimeoutMs()))
                .baseUrl(props.getStt().getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + props.getStt().getApiKey())
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
