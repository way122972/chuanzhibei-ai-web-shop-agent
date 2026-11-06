package com.shopguide.interceptor;

import com.shopguide.common.Result;
import com.shopguide.utils.JwtUtils;
import com.shopguide.utils.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT 登录拦截器
 *
 * 流程：取请求头 token → 校验签名/有效期 → 解析 JWT 载荷中的 userId、username
 *      → 写入 ThreadLocal（UserContext）→ 业务层直接取当前登录用户ID
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = request.getHeader("token");
        if (!StringUtils.hasText(token)) {
            return reject(response, "未登录，请先登录");
        }
        // 解析 JWT：签名错误或已过期都会返回 null
        Claims claims = jwtUtils.parseToken(token);
        if (claims == null) {
            return reject(response, "登录已过期，请重新登录");
        }
        // JWT 载荷中取出用户ID（登录时由 JwtUtils.generateToken 写入）
        Integer userId = claims.get("userId", Integer.class);
        String username = (String) claims.get("username");
        if (userId == null) {
            return reject(response, "令牌无效，请重新登录");
        }

        // 放入 ThreadLocal，后续 Service 层通过 UserContext.getRequiredUserId() 获取
        UserContext.setUserId(userId);
        UserContext.setUsername(username);
        // 同时放入 request，便于个别场景直接使用
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);
        return true;
    }

    /**
     * 请求结束后清理 ThreadLocal，防止线程池复用导致不同用户数据串号
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private boolean reject(HttpServletResponse response, String message) throws Exception {
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(401);
        response.getWriter().write(new ObjectMapper().writeValueAsString(Result.error(message)));
        return false;
    }
}
