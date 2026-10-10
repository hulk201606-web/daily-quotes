package com.example.dailyquotes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dailyquotes.domain.User;

/**
 * 用户数据访问接口。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper} 获得通用 CRUD 能力，
 * 主要用于按 id 解析「当前登录用户」以及列举全部用户（供前端切换身份演示）。</p>
 */
public interface UserMapper extends BaseMapper<User> {
}
