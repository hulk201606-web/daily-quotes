package com.example.dailyquotes.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 新增个人标签的请求体。
 *
 * <p>仅含标签名称，长度受控以避免超长；标签归属由服务端依据当前用户确定，
 * 不依赖前端传值，确保「个人标签」严格隔离在各自用户名下。</p>
 */
public record UserTagRequest(
        @NotBlank(message = "标签名称不能为空")
        @Size(max = 64, message = "标签名称不能超过 64 字")
        String name
) {
}
