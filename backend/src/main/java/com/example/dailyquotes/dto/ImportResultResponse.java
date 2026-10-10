package com.example.dailyquotes.dto;

/**
 * 批量导入结果响应。
 *
 * <p>用于「导入内置中文名人名言」等批量写入接口的返回，对外暴露新增条数、跳过重复条数
 * 与资源总条数，便于调用方确认导入效果。</p>
 */
public record ImportResultResponse(int inserted, int skipped, int total) {
}
