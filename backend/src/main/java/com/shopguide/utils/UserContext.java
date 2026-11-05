package com.shopguide.utils;

/**
 * 当前登录用户上下文（基于 ThreadLocal）
 *
 * 数据流转：
 *   1. 登录成功 → JwtUtils 把用户ID、用户名写入 JWT 载荷（claims）返回前端；
 *   2. 前端每次请求在请求头携带 token → JwtInterceptor 解析 JWT 取出 userId；
 *   3. 拦截器把 userId/username 放入 ThreadLocal；
 *   4. Service 层直接用 {@link #getRequiredUserId()} 取当前用户ID（购物车查询、添加等都靠它做数据隔离）；
 *   5. 请求结束（afterCompletion）必须 {@link #clear()}，否则线程池复用时会把上一个用户的数据带给下一个请求。
 *
 * 说明：项目采用 JWT 无状态鉴权，不在服务端保存会话文件/缓存，用户身份完全来自 JWT 载荷。
 */
public final class UserContext {

    /** 当前登录用户ID（对应 user 表主键） */
    private static final ThreadLocal<Integer> USER_ID = new ThreadLocal<>();

    /** 当前登录用户名 */
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setUserId(Integer userId) {
        USER_ID.set(userId);
    }

    public static Integer getUserId() {
        return USER_ID.get();
    }

    public static void setUsername(String username) {
        USERNAME.set(username);
    }

    public static String getUsername() {
        return USERNAME.get();
    }

    /**
     * 是否已登录（当前线程是否存在用户信息）
     */
    public static boolean isLogin() {
        return USER_ID.get() != null;
    }

    /**
     * 获取当前登录用户ID，未登录直接抛异常（业务层快速失败，避免用 null 去查库）
     */
    public static Integer getRequiredUserId() {
        Integer userId = USER_ID.get();
        if (userId == null) {
            throw new IllegalStateException("当前请求未登录，无法获取用户ID");
        }
        return userId;
    }

    /**
     * 获取当前登录用户名，未登录直接抛异常
     */
    public static String getRequiredUsername() {
        String username = USERNAME.get();
        if (username == null) {
            throw new IllegalStateException("当前请求未登录，无法获取用户名");
        }
        return username;
    }

    /**
     * 请求结束必须调用，清理当前线程的用户信息
     */
    public static void clear() {
        USER_ID.remove();
        USERNAME.remove();
    }
}
