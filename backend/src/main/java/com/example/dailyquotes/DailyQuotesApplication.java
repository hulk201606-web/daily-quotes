package com.example.dailyquotes;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 每日名言后端启动类。
 *
 * <p>作为 Spring Boot 应用入口，启用定时任务（每日采集）并扫描 MyBatis Mapper 接口
 * （{@code com.example.dailyquotes.mapper}），使 MyBatis-Plus 代理生效。</p>
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.example.dailyquotes.mapper")
public class DailyQuotesApplication {

    public static void main(String[] args) {
        SpringApplication.run(DailyQuotesApplication.class, args);
    }
}
