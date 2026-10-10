package com.example.dailyquotes.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 新增/编辑名言的请求体。
 *
 * <p>字段均带 Bean Validation 约束，保证内容、作者、出处非空且长度受控、知名度在 1-100 区间；
 * {@code featured} 表示是否精选（布尔）。该对象仅用于接收前端入参，落库时由服务层映射为 {@code Quote} 实体。</p>
 */
public record QuoteRequest(
        @NotBlank(message = "名言内容不能为空")
        @Size(max = 2000, message = "名言内容不能超过 2000 字")
        String content,

        @NotBlank(message = "作者不能为空")
        @Size(max = 120, message = "作者不能超过 120 字")
        String author,

        @NotBlank(message = "出处不能为空")
        @Size(max = 180, message = "出处不能超过 180 字")
        String source,

        @NotNull(message = "知名度不能为空")
        @Min(value = 1, message = "知名度不能低于 1")
        @Max(value = 100, message = "知名度不能高于 100")
        Integer popularity,

        boolean featured
) {
}
