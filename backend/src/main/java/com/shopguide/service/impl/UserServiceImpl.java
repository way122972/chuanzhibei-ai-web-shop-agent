package com.shopguide.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shopguide.common.Result;
import com.shopguide.entity.User;
import com.shopguide.mapper.UserMapper;
import com.shopguide.service.UserService;
import com.shopguide.utils.JwtUtils;
import com.shopguide.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 登录：数据库校验账号密码 → 生成 JWT（载荷包含 userId、username）返回前端
     * 服务端不保存任何会话文件，后续请求的用户身份全部来自 JWT 解析
     */
    @Override
    public Result<Map<String, Object>> login(String username, String password) {
        if (username == null || username.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {
            return Result.error("账号和密码不能为空");
        }
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq("username", username.trim());
        User user = userMapper.selectOne(qw);
        if (user == null || !user.getPassword().equals(password)) {
            return Result.error("账号或密码错误");
        }

        // 把用户ID、用户名写进 JWT，前端后续请求带回，后端解析后放入 ThreadLocal
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("money", user.getMoney());
        return Result.success("登录成功", data);
    }

    @Override
    public Result<Map<String, Object>> getUserInfo() {
        // 当前登录用户ID来自 ThreadLocal（由 JwtInterceptor 解析 JWT 后写入）
        Integer userId = UserContext.getRequiredUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("money", user.getMoney());
        return Result.success(data);
    }
}
