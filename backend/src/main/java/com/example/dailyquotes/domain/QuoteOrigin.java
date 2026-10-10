package com.example.dailyquotes.domain;

public enum QuoteOrigin {
    /** 系统内置示例数据 */
    SEED,
    /** 互联网每日采集 */
    INTERNET,
    /** 运营后台人工维护 */
    MANUAL,
    /** 内置中文名人名言（离线资源导入） */
    CN_SEED,
    /** 在线中文名人名言（hitokoto / 清和 / 客官 / 需 key 源等） */
    CN_INTERNET
}
