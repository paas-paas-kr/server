package com.chat.config;

import com.chat.audio.AudioWebSocketHandler;
import com.chat.chat.ChatWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;

import java.util.Map;

@Configuration
public class WebSocketConfig implements WebFluxConfigurer {

    @Bean
    public WebSocketService webSocketServcie(){
        return new HandshakeWebSocketService(new ReactorNettyRequestUpgradeStrategy());
    }

    @Bean
    public WebSocketHandlerAdapter webSocketHandlerAdapter(WebSocketService service){
        return new WebSocketHandlerAdapter(service);
    }

    @Bean
    public SimpleUrlHandlerMapping webSocketHandlerMapping(ChatWebSocketHandler chatWebSocketHandler, AudioWebSocketHandler audioWebSocketHandler/*,SttWebSocketHandler sttWsHandler*/){
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(Map.of(
                "/ws/chat" , chatWebSocketHandler,
                "/ws/audio", audioWebSocketHandler

                /*,
                "/ws/stt", sttWsHandler*/
        ));
        mapping.setOrder(-1);
        return mapping;
    }



}
