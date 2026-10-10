package com.example.dailyquotes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dailyquotes.domain.Quote;
import com.example.dailyquotes.domain.UserQuoteTag;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 名言-个人标签关联数据访问接口。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper} 获得通用 CRUD；
 * 另提供「按用户+标签分页列举被打标名言」的联表查询，支撑「按个人喜好标签浏览收录」。</p>
 */
public interface UserQuoteTagMapper extends BaseMapper<UserQuoteTag> {

    /** 统计某用户在某标签下收录的名言数量，用于分页元信息。 */
    @Select("SELECT COUNT(1) FROM user_quote_tag WHERE user_id = #{userId} AND tag_id = #{tagId}")
    Long countByUserAndTag(@Param("userId") Long userId, @Param("tagId") Long tagId);

    /** 按用户+标签分页列举被打标的名言（按关联时间倒序）。 */
    @Select("SELECT q.* FROM quote q INNER JOIN user_quote_tag ut ON q.id = ut.quote_id " +
            "WHERE ut.user_id = #{userId} AND ut.tag_id = #{tagId} ORDER BY ut.id DESC LIMIT #{size} OFFSET #{offset}")
    List<Quote> selectQuotesByTag(@Param("userId") Long userId,
                                  @Param("tagId") Long tagId,
                                  @Param("size") int size,
                                  @Param("offset") long offset);
}
