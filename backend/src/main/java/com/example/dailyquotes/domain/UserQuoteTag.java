package com.example.dailyquotes.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 名言-个人标签关联实体（对应数据库表 {@code user_quote_tag}）。
 *
 * <p>将用户个人的 {@link UserTag} 打在某条 {@link Quote} 上，实现「按个人标签归类收录」。
 * 同一用户、同一名言、同一标签至多关联一次（库表以 {@code (user_id, quote_id, tag_id)} 唯一约束保障），
 * 该关系是纯个人视角，不同用户之间互不可见、互不干扰。</p>
 */
@TableName("user_quote_tag")
public class UserQuoteTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long quoteId;

    private Long tagId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public UserQuoteTag() {
    }

    public UserQuoteTag(Long userId, Long quoteId, Long tagId) {
        this.userId = userId;
        this.quoteId = quoteId;
        this.tagId = tagId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getQuoteId() {
        return quoteId;
    }

    public void setQuoteId(Long quoteId) {
        this.quoteId = quoteId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
