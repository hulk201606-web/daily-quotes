package com.example.dailyquotes.controller;

import com.example.dailyquotes.config.CurrentUser;
import com.example.dailyquotes.domain.User;
import com.example.dailyquotes.dto.UserResponse;
import com.example.dailyquotes.security.AuthUtils;
import com.example.dailyquotes.service.UserService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户相关接口。
 *
 * <p>当前系统采用轻量身份标识（请求头 {@code X-User-Id}），本控制器提供：
 * <ul>
 *   <li>{@code GET /api/users}——列举全部用户（脱敏），供前端「切换身份 / 模拟登录」演示；</li>
 *   <li>{@code GET /api/users/me}——返回当前登录用户自身的脱敏信息。</li>
 * </ul>
 * 权限控制统一由 {@link AuthUtils} 负责，本控制器仅暴露只读信息。</p>
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 列举全部用户（脱敏），供前端切换身份使用。 */
    @GetMapping
    public List<UserResponse> listUsers() {
        return userService.listUsers();
    }

    /** 返回当前登录用户的信息；未携带有效用户标识时返回 401。 */
    @GetMapping("/me")
    public UserResponse me(@CurrentUser User user) {
        AuthUtils.requireUser(user);
        return userService.getCurrentUser(user.getId());
    }
}
