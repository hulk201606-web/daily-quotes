package com.example.dailyquotes.dto;

import com.example.dailyquotes.domain.UserTag;

/**
 * 个人标签对外响应对象。
 *
 * <p>由 {@link #from(UserTag)} 转换而来，对外暴露标签 id 与名称，
 * 用于个人标签的列举、创建与打标场景。</p>
 */
public record UserTagResponse(
        Long id,
        String name
) {
    /** 将标签实体转换为对外响应对象。 */
    public static UserTagResponse from(UserTag tag) {
        return new UserTagResponse(tag.getId(), tag.getName());
    }
}
