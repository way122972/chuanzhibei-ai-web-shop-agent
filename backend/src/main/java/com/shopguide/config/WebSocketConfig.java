package com.shopguide.config;

import com.shopguide.ws.AiWebSocketHandler;
import com.shopguide.ws.WsTokenHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置：AI 助手通道
 *
 * 端点：ws://localhost:8081/ws/ai?token=<JWT>
 * 握手时由 WsTokenHandshakeInterceptor 校验 token，通过后才建立连接。
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private AiWebSocketHandler aiWebSocketHandler;

    @Autowired
    private WsTokenHandshakeInterceptor wsTokenHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(aiWebSocketHandler, "/ws/ai")
                .addInterceptors(wsTokenHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
