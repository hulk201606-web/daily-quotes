package com.example.dailyquotes.dto;

import com.example.dailyquotes.domain.User;

/**
 * 用户对外响应对象。
 *
 * <p>由 {@link #from(User)} 转换而来，对外暴露 id、用户名、显示名与角色，
 * 供前端展示当前用户与「切换身份（模拟登录）」时使用。密码等敏感信息一律不对外暴露。</p>
 */
public record UserResponse(
        Long id,
        String username,
        String displayName,
        String role
) {
    /** 将用户实体转换为对外响应对象。 */
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());
    }
}
