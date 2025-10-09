package com.chat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("app")
public class AppProperties {
    private Stt stt = new Stt();
    private Llm llm = new Llm();
    private Tts tts = new Tts();

    @Data public static class Stt { private String baseUrl; private String apiKey; private int timeoutMs; }
    @Data public static class Llm { private String baseUrl; private String apiKey; private int timeoutMs; }
    @Data public static class Tts { private String baseUrl; private String apiKey; private int timeoutMs; }
}
