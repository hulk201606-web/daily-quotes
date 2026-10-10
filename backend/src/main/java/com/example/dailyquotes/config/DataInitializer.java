package com.example.dailyquotes.config;

import com.example.dailyquotes.domain.Quote;
import com.example.dailyquotes.domain.QuoteOrigin;
import com.example.dailyquotes.domain.User;
import com.example.dailyquotes.mapper.QuoteMapper;
import com.example.dailyquotes.mapper.UserMapper;
import com.example.dailyquotes.service.ChineseQuoteImporter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 应用启动时的种子数据初始化。
 *
 * <p>分为两部分，均在数据库为空时写入，已存在数据则跳过：
 * <ol>
 *   <li>名言种子——写入 10 条内置示例名言（来源 SEED），便于系统开箱即用、首屏即有内容；</li>
 *   <li>用户种子——写入一名管理员（admin）与若干普通用户，用于演示「个人标签/喜好收录」
 *       与「仅管理员可写、其余人只读」的权限模型；前端可通过切换 {@code X-User-Id} 模拟不同身份。</li>
 * </ol>
 * 标记 {@code @Order(1)} 确保在其它依赖数据的启动逻辑之前执行。</p>
 */
@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataInitializer.class);

    private final QuoteMapper quoteMapper;
    private final UserMapper userMapper;
    private final ChineseQuoteImporter chineseQuoteImporter;

    public DataInitializer(QuoteMapper quoteMapper, UserMapper userMapper, ChineseQuoteImporter chineseQuoteImporter) {
        this.quoteMapper = quoteMapper;
        this.userMapper = userMapper;
        this.chineseQuoteImporter = chineseQuoteImporter;
    }

    @Override
    public void run(String... args) {
        if (quoteMapper.selectCount(null) > 0) {
            LOGGER.info("[DB初始化] 检测到已有名言数据，跳过种子写入");
        } else {
            LOGGER.info("[DB初始化] 写入 10 条内置示例名言");
            for (Quote quote : buildSeedQuotes()) {
                quoteMapper.insert(quote);
            }
        }

        // 内置中文名人名言：幂等导入，每次启动补充，已存在则跳过
        chineseQuoteImporter.importBuiltIn();

        if (userMapper.selectCount(null) > 0) {
            LOGGER.info("[DB初始化] 检测到已有用户数据，跳过用户种子写入");
        } else {
            LOGGER.info("[DB初始化] 写入管理员与示例用户");
            for (User user : buildSeedUsers()) {
                userMapper.insert(user);
            }
        }
    }

    private List<Quote> buildSeedQuotes() {
        Object[][] seeds = {
            {"读书患不多，思义患不明。", "韩愈", "劝学诗", 96, true},
            {"古之立大事者，不惟有超世之才，亦必有坚忍不拔之志。", "苏轼", "晁错论", 95, true},
            {"纸上得来终觉浅，绝知此事要躬行。", "陆游", "冬夜读书示子聿", 94, true},
            {"问渠那得清如许？为有源头活水来。", "朱熹", "观书有感", 92, false},
            {"业精于勤，荒于嬉；行成于思，毁于随。", "韩愈", "进学解", 90, false},
            {"博学之，审问之，慎思之，明辨之，笃行之。", "子思", "中庸", 89, false},
            {"黑发不知勤学早，白首方悔读书迟。", "颜真卿", "劝学", 88, false},
            {"非淡泊无以明志，非宁静无以致远。", "诸葛亮", "诫子书", 91, false},
            {"千里之行，始于足下。", "老子", "道德经", 93, false},
            {"三军可夺帅也，匹夫不可夺志也。", "孔子", "论语", 90, false}
        };
        List<Quote> quotes = new ArrayList<>();
        for (Object[] s : seeds) {
            Quote quote = new Quote();
            quote.setContent((String) s[0]);
            quote.setAuthor((String) s[1]);
            quote.setSource((String) s[2]);
            quote.setPopularity((Integer) s[3]);
            quote.setFeatured((Boolean) s[4]);
            quote.setOrigin(QuoteOrigin.SEED.name());
            quote.setLang("zh");
            quote.setFingerprint(Quote.fingerprintOf(quote.getContent(), quote.getAuthor()));
            quotes.add(quote);
        }
        return quotes;
    }

    /**
     * 种子用户：管理员 {@code admin}（id=1，可写）与两名普通用户（id=2/3，仅可读、可维护个人标签与收藏）。
     * 主键采用 INPUT 策略，固定 id 便于演示时直接作为 {@code X-User-Id} 使用。
     */
    private List<User> buildSeedUsers() {
        List<User> users = new ArrayList<>();
        users.add(new User(1L, "admin", "管理员", User.ROLE_ADMIN));
        users.add(new User(2L, "zhangsan", "张三", User.ROLE_USER));
        users.add(new User(3L, "lisi", "李四", User.ROLE_USER));
        return users;
    }
}
