package com.example.dailyquotes.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 全局配置。
 *
 * <p>注册分页插件（PaginationInnerInterceptor），未显式指定数据库类型时，
 * 框架会依据当前数据源连接元信息自动识别 H2 / MySQL / PostgreSQL 方言，
 * 从而实现「一份代码、多库适配」。</p>
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 未指定数据库类型时，由 MyBatis-Plus 根据连接元信息自动识别 H2 / MySQL / PostgreSQL
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }
}
