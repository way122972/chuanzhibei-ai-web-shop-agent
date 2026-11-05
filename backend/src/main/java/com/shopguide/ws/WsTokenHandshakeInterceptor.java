package com.shopguide.ws;

import com.shopguide.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * WebSocket 握手鉴权
 *
 * 浏览器建立 WebSocket 时无法自定义请求头，因此前端把 JWT 放在查询参数里：
 *   ws://host:8081/ws/ai?token=xxx
 * 握手阶段解析 token，把 userId / username 放进 session 属性，后续消息处理直接用，
 * 不依赖 ThreadLocal（流式回调可能运行在模型 SDK 的线程上）。
 */
@Slf4j
@Component
public class WsTokenHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI()).build()
                .getQueryParams().getFirst("token");
        if (!StringUtils.hasText(token)) {
            // 兼容把 token 放在请求头里的场景
            token = request.getHeaders().getFirst("token");
        }
        Claims claims = StringUtils.hasText(token) ? jwtUtils.parseToken(token) : null;
        if (claims == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            log.warn("AI WebSocket 握手失败：token 缺失或无效");
            return false;
        }
        attributes.put("userId", Integer.valueOf(String.valueOf(claims.get("userId"))));
        attributes.put("username", String.valueOf(claims.get("username")));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 无需处理
    }
}
