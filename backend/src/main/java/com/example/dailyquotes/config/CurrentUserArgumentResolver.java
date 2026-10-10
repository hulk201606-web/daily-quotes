package com.example.dailyquotes.config;

import com.example.dailyquotes.domain.User;
import com.example.dailyquotes.mapper.UserMapper;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;

/**
 * 「当前用户」方法参数解析器。
 *
 * <p>仅对标记了 {@link CurrentUser} 且类型为 {@link User} 的参数生效：
 * 从 HTTP 请求头 {@code X-User-Id} 取出用户 id，查询 {@link UserMapper} 得到用户实体后注入。
 * 由于当前系统未引入独立鉴权中心，采用「请求头携带用户 id」的轻量方式标识身份，
 * 便于前端通过切换 {@code X-User-Id} 模拟不同用户（含管理员）登录；
 * 该头缺失或 id 无效时返回 {@code null}，交由此后业务逻辑判读是否允许匿名。</p>
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserMapper userMapper;

    public CurrentUserArgumentResolver(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && User.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    @Nullable
    public Object resolveArgument(MethodParameter parameter,
                                  @Nullable ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  @Nullable WebDataBinderFactory binderFactory) {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return null;
        }
        try {
            Long userId = Long.valueOf(userIdHeader.trim());
            return userMapper.selectById(userId);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
