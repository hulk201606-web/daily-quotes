package com.example.dailyquotes.security;

import com.example.dailyquotes.domain.User;
import com.example.dailyquotes.exception.ForbiddenException;
import com.example.dailyquotes.exception.UnauthorizedException;

/**
 * 轻量级权限守卫。
 *
 * <p>集中封装两类常见鉴权判断，避免在各个 Controller / Service 中散落重复的权限校验逻辑：
 * <ul>
 *   <li>{@link #requireUser(User)}——要求必须已识别有效用户（个人标签/收藏等接口的前置条件）；</li>
 *   <li>{@link #requireAdmin(User)}——要求当前用户为管理员（名言写操作、每日采集的前置条件）。</li>
 * </ul>
 * 鉴权不通过时分别抛出 {@link UnauthorizedException}（401）或 {@link ForbiddenException}（403）。</p>
 */
public final class AuthUtils {

    private AuthUtils() {
    }

    /**
     * 要求请求已识别为有效用户，否则视为未登录。
     *
     * @param user 由 {@code @CurrentUser} 解析出的用户（可能为 null）
     * @return 非空的用户实体，便于链式使用
     */
    public static User requireUser(User user) {
        if (user == null) {
            throw new UnauthorizedException("请先以用户身份访问（缺少 X-User-Id）");
        }
        return user;
    }

    /**
     * 要求当前用户具备管理员角色，否则禁止其执行写操作。
     *
     * <p>这正是需求中「除管理员外其他人员只能查看、不能修改删除」的落地点：
     * 任何对公共名言的增删改、以及触发采集的接口，都必须先经过本校验。</p>
     *
     * @param user 由 {@code @CurrentUser} 解析出的用户（可能为 null）
     */
    public static void requireAdmin(User user) {
        if (user == null || !user.isAdmin()) {
            throw new ForbiddenException("仅管理员可进行新增、修改、删除等操作");
        }
    }
}
