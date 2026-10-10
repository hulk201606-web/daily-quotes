package com.example.dailyquotes.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 名言主表实体（对应数据库表 {@code quote}）。
 *
 * <p>该实体同时承载「系统内置示例」「互联网每日采集」「运营人工维护」三种来源的数据，
 * 通过 {@link #origin} 字段区分来源，通过 {@link #fingerprint} 字段（内容+作者的 SHA-256 摘要）
 * 在应用层与数据库唯一约束双重保障下去重，避免同一名言被重复写入。</p>
 */
@TableName("quote")
public class Quote {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String content;

    private String author;

    private String source;

    /** 知名度 1-100 */
    private Integer popularity;

    /** 是否精选：0 否 1 是 */
    private Boolean featured;

    /** 内容来源 */
    private String origin;

    /** 去重指纹（内容+作者 的 SHA-256 摘要，用于跨库唯一约束） */
    private String fingerprint;

    /** 最近一次采集日期（互联网来源按天去重与限量使用） */
    private LocalDate lastFetchedDate;

    /** 翻译前原文：外文采集并翻译为中文后留存原始外文，未翻译时为 NULL */
    private String originalContent;

    /** 内容语种：zh=中文 / en=外文（翻译后 content 为中文、originalContent 存原文） */
    private String lang;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public Quote() {
    }

    /**
     * 生成去重指纹：对「内容 + 作者」拼接字符串计算 SHA-256 摘要。
     *
     * <p>指纹用于判断两条名言是否为同一内容（忽略热度、出处等附加信息），
     * 数据库对 {@code fingerprint} 字段建立了唯一约束，插入重复数据时会触发唯一键冲突。</p>
     *
     * @param content 名言内容（为空时按空串处理）
     * @param author  作者（为空时按空串处理）
     * @return 64 位十六进制摘要字符串
     */
    public static String fingerprintOf(String content, String author) {
        String raw = (content == null ? "" : content) + "||" + (author == null ? "" : author);
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Integer getPopularity() {
        return popularity;
    }

    public void setPopularity(Integer popularity) {
        this.popularity = popularity;
    }

    public Boolean getFeatured() {
        return featured;
    }

    public void setFeatured(Boolean featured) {
        this.featured = featured;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public LocalDate getLastFetchedDate() {
        return lastFetchedDate;
    }

    public void setLastFetchedDate(LocalDate lastFetchedDate) {
        this.lastFetchedDate = lastFetchedDate;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
