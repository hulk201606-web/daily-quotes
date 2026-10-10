package com.example.dailyquotes.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.example.dailyquotes.domain.Quote;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 名言数据访问接口。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper} 获得通用 CRUD 能力，
 * 同时针对去重、统计、展示等场景补充了若干自定义查询方法。</p>
 */
public interface QuoteMapper extends BaseMapper<Quote> {

    /** 查询全部非空的去重出处列表，用于前端筛选下拉框。 */
    @Select("SELECT DISTINCT source FROM quote WHERE source IS NOT NULL AND source <> '' ORDER BY source")
    List<String> selectDistinctSources();

    /** 按指纹统计同名名言数量，为 0 表示当前内容+作者组合不存在。 */
    @Select("SELECT COUNT(1) FROM quote WHERE fingerprint = #{fingerprint}")
    Integer countByFingerprint(@Param("fingerprint") String fingerprint);

    /** 业务语义封装：判断指定指纹的名言是否已存在（用于写入前去重校验）。 */
    default boolean existsByFingerprint(String fingerprint) {
        return countByFingerprint(fingerprint) > 0;
    }

    /** 按来源（SEED/INTERNET/MANUAL）统计名言数量，用于概览统计。 */
    @Select("SELECT COUNT(1) FROM quote WHERE origin = #{origin}")
    Integer countByOrigin(@Param("origin") String origin);

    /** 统计指定来源且采集日期为当天的名言数量（用于「今日采集」指标）。 */
    @Select("SELECT COUNT(1) FROM quote WHERE origin = #{origin} AND last_fetched_date = #{date}")
    Integer countByOriginAndFetchedDate(@Param("origin") String origin, @Param("date") LocalDate date);

    /** 查询全部精选名言，按知名度与主键倒序，可用于首页焦点区展示。 */
    @Select("SELECT * FROM quote WHERE featured = 1 ORDER BY popularity DESC, id DESC")
    List<Quote> selectFeatured();

    /** 通用条件查询：将 MyBatis-Plus 的 {@link Wrapper} 拼接到 SQL 中，支持复杂筛选。 */
    @Select("SELECT * FROM quote ${ew.customSqlSegment}")
    List<Quote> selectListWithCondition(@Param(Constants.WRAPPER) Wrapper<Quote> wrapper);
}
