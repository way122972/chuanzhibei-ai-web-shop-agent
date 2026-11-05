package com.shopguide.controller;

import com.shopguide.common.Result;
import com.shopguide.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 登录接口
 */
@RestController
@RequestMapping("/user")
public class LoginController {

    @Autowired
    private UserService userService;

    /**
     * 登录：POST /user/login  body: {username, password}
     * 校验通过返回 {token, userId, username, money}
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> loginForm) {
        return userService.login(loginForm.get("username"), loginForm.get("password"));
    }

    /**
     * 当前登录用户信息（含账户余额）：GET /user/info
     * 用户身份由 JWT 解析得到，无需前端传参
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        return userService.getUserInfo();
    }
}
