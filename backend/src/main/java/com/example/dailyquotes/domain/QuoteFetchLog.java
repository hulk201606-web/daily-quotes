package com.example.dailyquotes.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采集任务执行记录实体（对应数据库表 {@code quote_fetch_log}）。
 *
 * <p>每次「每日采集」任务（无论定时触发还是手动触发）执行结束后都会写入一条记录，
 * 用于审计采集结果、统计成功/失败次数，以及保证「同一业务日期仅成功采集一次」。
 * 通过 {@code fetch_date} 业务日期维度与采集服务中的去重判断配合实现每日限量。</p>
 */
@TableName("quote_fetch_log")
public class QuoteFetchLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务日期（按天去重，每日仅记录一条） */
    private LocalDate fetchDate;

    /** 触发方式：SCHEDULED 定时 / MANUAL 手动 */
    private String fetchType;

    /** 成功写入条数 */
    private Integer fetchedCount;

    /** 因重复跳过条数 */
    private Integer skippedCount;

    /** 采集尝试次数 */
    private Integer totalAttempts;

    /** SUCCESS / FAILED */
    private String status;

    /** 失败时的错误信息 */
    private String errorMessage;

    private LocalDateTime createdAt;

    public QuoteFetchLog() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getFetchDate() {
        return fetchDate;
    }

    public void setFetchDate(LocalDate fetchDate) {
        this.fetchDate = fetchDate;
    }

    public String getFetchType() {
        return fetchType;
    }

    public void setFetchType(String fetchType) {
        this.fetchType = fetchType;
    }

    public Integer getFetchedCount() {
        return fetchedCount;
    }

    public void setFetchedCount(Integer fetchedCount) {
        this.fetchedCount = fetchedCount;
    }

    public Integer getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(Integer skippedCount) {
        this.skippedCount = skippedCount;
    }

    public Integer getTotalAttempts() {
        return totalAttempts;
    }

    public void setTotalAttempts(Integer totalAttempts) {
        this.totalAttempts = totalAttempts;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
