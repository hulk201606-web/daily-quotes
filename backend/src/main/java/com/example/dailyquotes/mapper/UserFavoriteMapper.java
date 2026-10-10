package com.example.dailyquotes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dailyquotes.domain.Quote;
import com.example.dailyquotes.domain.UserFavorite;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户个人收藏数据访问接口。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper} 获得通用 CRUD；
 * 另提供「按用户分页列举其收藏的名言」所需的关联查询（与 {@code quote} 表联表）。</p>
 */
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {

    /** 统计某用户的收藏总数，用于分页元信息。 */
    @Select("SELECT COUNT(1) FROM user_favorite WHERE user_id = #{userId}")
    Long countByUser(@Param("userId") Long userId);

    /** 按用户分页列举其收藏的名言（按收藏时间倒序），用于「我的收藏」列表。 */
    @Select("SELECT q.* FROM quote q INNER JOIN user_favorite f ON q.id = f.quote_id " +
            "WHERE f.user_id = #{userId} ORDER BY f.id DESC LIMIT #{size} OFFSET #{offset}")
    List<Quote> selectFavoriteQuotes(@Param("userId") Long userId,
                                     @Param("size") int size,
                                     @Param("offset") long offset);
}
