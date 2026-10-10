package com.example.dailyquotes.dto;

import javax.validation.constraints.NotNull;

/**
 * 给名言打个人标签的请求体。
 *
 * <p>仅携带目标标签 id，标签归属由服务端依据当前用户判定（标签必须属于当前用户），
 * 不允许前端越权指定他人标签。</p>
 */
public record QuoteTagRequest(
        @NotNull(message = "标签 id 不能为空")
        Long tagId
) {
}
