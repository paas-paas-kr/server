package com.chat.config;

import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;


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
    private final GoogleAccessTokenProvider tokenProvider;

    /**
     * HttpClient (Reactor Netty의 저수준 논블로킹 클라이언트)
     * 역할: 실제 소켓/이벤트루프/커넥션풀/TLS/타임아웃 같은 네트워크 레벨을 다룬다.
     * -> 각종 타임아웃과 채널 핸들러를 심어 놓고, 그걸 WebClient가 쓰도록 어댑터(ReactorClientHttpConnector)로 감싸서 반환
     *-> sttWebClient()가 내부에서 쓰는 HttpClient에 옵션이 결려있으면, 그 WebClient가 만드는 모든 신규 소켓 연결에 동일하게 적용
     *
     *connectTimeoutMs: TCP 연결이 완료될 때까지 기다리는 시간(보통 3000ms 정도)
     * readTimeoutMs: 요청을 보낸 뒤 서버 응답(첫 바이트/헤더)를 받을 때까지 기다리는 시간(STT는 60000~90000ms 권장)
     */
    private ReactorClientHttpConnector connector(int connectTimeoutMs,int readTimeoutMs) {
        HttpClient http = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)   // TCP 연결 타임아웃
                .responseTimeout(java.time.Duration.ofMillis(readTimeoutMs));     // 응답 대기 타임아웃

        return new ReactorClientHttpConnector(http);

    }

    @Bean
    public WebClient sttWebClient(){
        return WebClient.builder()
                .baseUrl(props.getStt().getBaseUrl())
                .clientConnector(connector(props.getStt().getConnectTimeoutMs(),props.getStt().getReadTimeoutMs()))
                .build();
    }

    @Bean
    public WebClient transWebClient(){
        return WebClient.builder()
                .baseUrl(props.getTrans().getBaseUrl())
                .clientConnector(connector(props.getTrans().getConnectTimeoutMs(),props.getTrans().getReadTimeoutMs()))
                .build();
    }



    @Bean
    public WebClient llmWebClient() {
        return WebClient.builder()
                .baseUrl(props.getLlm().getBaseUrl())
                .clientConnector(connector(props.getLlm().getConnectionTimeoutMs(),props.getLlm().getReadTimeoutMs()))
                .build();
    }

    @Bean
    public WebClient  vertexWebClient() {
        return WebClient.builder()
                .baseUrl(props.getVertex().getSearchBaseUrl())
                .clientConnector(connector(props.getVertex().getHttp().getConnectionTimeoutMs(), props.getVertex().getHttp().getReadTimeoutMs()))
                /**
                 * WebClient 인스턴스가 생성하는 모든 HTTP 요청(Request)을 실행하기 직전에 가로채서(intecept) 특정 로직을 수행하게 하는 기능
                 * 앞으로 vertexWebClient 를 사용하는 모든 요청을 가로채서, 자동으로 토큰을 붙인 뒤, 원래 하려던 요청을 계속 진행시켜라라는 공통 규칙을 설정
                 * req (ClientRequest): post(), get()등으로 만들어진 원본 요청 객체
                 * -> 이 객체에는 URL, HTTP 메서드, 원본 헤더, 전송할 본문(body) 등의 정보가 들어있다.
                 * next.exchange(...)를 호출해야만 요청이 다음 단계로 진행된다.
                 *
                 * ClientRequest.from(req) : ClientRequest 객체는 불변(immutable)이다. 즉, 생성한 req 객체를 직접 수정할 수 없다.
                 * -> 따라서 ClientRequest.from(req)를 사용해, 원본 req 의 모든 속성(URL, 메서드 , 본문 등)을 그대로 복사한 새로운 ClientRequest.Builder를 생성한다.
                 * tokenProvider.getBearerToken()를 호출하여 GoogleAccessTokenProvider로부터 현재 유효한 액세스 토큰 문자열을 가져온다.
                 * h.setBearerAuth(...)는 HTTP 헤더에 Authorization: Bearer <가져온_토큰값> 항목을 추가하거나 덮어쓴다.
                 */
                .filter((req, next) ->
                        next.exchange(
                                ClientRequest.from(req)
                                        .headers(h -> h.setBearerAuth(tokenProvider.getBearerToken()))
                                        .build()))
                .build();
    }



}
