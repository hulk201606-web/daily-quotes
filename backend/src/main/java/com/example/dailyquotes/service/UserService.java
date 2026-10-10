package com.example.dailyquotes.service;

import com.example.dailyquotes.domain.User;
import com.example.dailyquotes.dto.UserResponse;
import com.example.dailyquotes.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户查询服务。
 *
 * <p>提供「按 id 查询用户」与「列举全部用户」能力。
 * 当前系统采用轻量身份标识（请求头 {@code X-User-Id}），列举全部用户主要用于前端
 * 「切换身份 / 模拟登录」演示；权限判断由 {@code AuthUtils} 统一负责。</p>
 */
@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /** 列举全部用户（脱敏后），供前端切换身份使用。 */
    public List<UserResponse> listUsers() {
        LOGGER.info("[DB查询] 列举全部用户");
        return userMapper.selectList(null).stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    /** 按 id 查询单个用户的脱敏信息（用于「我的信息」）。 */
    public UserResponse getCurrentUser(Long userId) {
        LOGGER.info("[DB查询] 查询当前用户信息 id={}", userId);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new com.example.dailyquotes.exception.ResourceNotFoundException("未找到 id=" + userId + " 的用户");
        }
        return UserResponse.from(user);
    }
}
