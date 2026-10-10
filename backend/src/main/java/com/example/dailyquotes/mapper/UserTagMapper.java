package com.example.dailyquotes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dailyquotes.domain.UserTag;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户个人标签数据访问接口。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper} 获得通用 CRUD；
 * 另提供按用户统计、按用户+名称去重查询等个性化方法。</p>
 */
public interface UserTagMapper extends BaseMapper<UserTag> {

    /** 判断某用户下是否已存在同名标签（用于新增前去重校验）。 */
    @Select("SELECT COUNT(1) FROM user_tag WHERE user_id = #{userId} AND name = #{name}")
    Integer countByUserAndName(@Param("userId") Long userId, @Param("name") String name);
}
