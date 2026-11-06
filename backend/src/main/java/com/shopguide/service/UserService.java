package com.shopguide.service;

import com.shopguide.common.Result;

import java.util.Map;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 登录：校验数据库中的账号密码，通过则返回携带用户ID的 JWT 令牌
     */
    Result<Map<String, Object>> login(String username, String password);

    /**
     * 获取当前登录用户信息（用户名、账户余额）
     * 当前用户ID从 ThreadLocal（UserContext）获取
     */
    Result<Map<String, Object>> getUserInfo();
}
