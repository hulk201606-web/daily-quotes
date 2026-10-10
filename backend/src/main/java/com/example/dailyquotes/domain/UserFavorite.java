package com.example.dailyquotes.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 用户个人收藏（收录）实体（对应数据库表 {@code user_favorite}）。
 *
 * <p>记录「某用户将某条名言收进个人收藏夹」的关系，体现需求中的「个人喜好进行收录」。
 * 同一用户对同一名言至多收藏一次（库表以 {@code (user_id, quote_id)} 唯一约束保障），
 * 收藏与否仅属于个人视角，不影响公共名言数据，也不受管理员角色限制。</p>
 */
@TableName("user_favorite")
public class UserFavorite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long quoteId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public UserFavorite() {
    }

    public UserFavorite(Long userId, Long quoteId) {
        this.userId = userId;
        this.quoteId = quoteId;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
