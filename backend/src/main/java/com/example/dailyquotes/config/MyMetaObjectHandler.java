package com.example.dailyquotes.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * MyBatis-Plus 字段自动填充处理器。
 *
 * <p>在插入/更新时自动维护审计字段：
 * 插入时填充 {@code createdAt}/{@code updatedAt}，且当来源为 INTERNET 时额外填充 {@code lastFetchedDate}；
 * 更新时仅刷新 {@code updatedAt}。从而保证时间字段由框架统一控制，避免业务代码遗漏。</p>
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        // 互联网来源自动记录首次采集日期；仅当实体存在 origin 字段时才处理，避免其它实体（如用户）报错
        if (metaObject.hasGetter("origin")) {
            Object origin = metaObject.getValue("origin");
            if ("INTERNET".equals(origin)) {
                strictInsertFill(metaObject, "lastFetchedDate", LocalDate.class, LocalDate.now());
            }
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
