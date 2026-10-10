package com.example.dailyquotes.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 用户实体（对应数据库表 {@code sys_user}）。
 *
 * <p>系统引入的最小用户模型，用于支撑「按个人标签 / 个人喜好收录」与「角色权限控制」两大能力：
 * <ul>
 *   <li>{@code role} 区分管理员（ADMIN）与普通用户（USER），决定能否对名言进行写操作；</li>
 *   <li>普通用户仅可浏览名言，并维护属于自己的标签与收藏，不参与公共数据的修改。</li>
 * </ul>
 * 主键采用 {@code INPUT} 策略，便于种子数据在初始化时固定管理员 id，便于演示与联调。</p>
 */
@TableName("sys_user")
public class User {

    /** 角色：管理员，拥有名言的增删改与采集权限。 */
    public static final String ROLE_ADMIN = "ADMIN";
    /** 角色：普通用户，仅可浏览，并维护个人标签与收藏。 */
    public static final String ROLE_USER = "USER";

    @TableId(type = IdType.INPUT)
    private Long id;

    private String username;

    private String displayName;

    private String role;

    @com.baomidou.mybatisplus.annotation.TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createdAt;

    public User() {
    }

    public User(Long id, String username, String displayName, String role) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.role = role;
    }

    /** 判断当前用户是否为管理员（角色为 ADMIN 即视为管理员）。 */
    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
