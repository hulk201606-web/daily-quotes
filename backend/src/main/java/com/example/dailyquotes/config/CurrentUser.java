package com.example.dailyquotes.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记 Controller 方法参数为「当前登录用户」。
 *
 * <p>配合 {@link CurrentUserArgumentResolver} 使用：框架在解析该参数时，
 * 会从请求头 {@code X-User-Id} 读取用户 id 并加载对应用户实体。
 * 若请求未携带该头或用户不存在，则注入 {@code null}（表示匿名访问），
 * 由具体接口自行决定是否允许匿名以及是否要求管理员权限。</p>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
