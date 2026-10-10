package com.example.dailyquotes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dailyquotes.domain.QuoteFetchLog;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

/**
 * 采集任务日志数据访问接口，继承 MyBatis-Plus 的 {@link BaseMapper} 获得通用 CRUD 能力。
 */
public interface QuoteFetchLogMapper extends BaseMapper<QuoteFetchLog> {

    /** 按业务日期统计当日是否已存在采集记录，用于实现「每日仅成功采集一次」的限量控制（兼容历史任意状态）。 */
    @Select("SELECT COUNT(1) FROM quote_fetch_log WHERE fetch_date = #{date}")
    Integer countByFetchDate(@org.apache.ibatis.annotations.Param("date") LocalDate date);

    /** 按业务日期+成功状态统计，仅当当天已「成功」采集过才拦截定时任务；FAILED/0 条不阻断重试。 */
    @Select("SELECT COUNT(1) FROM quote_fetch_log WHERE fetch_date = #{date} AND status = 'SUCCESS'")
    Integer countSuccessByFetchDate(@org.apache.ibatis.annotations.Param("date") LocalDate date);
}
