package com.example.dailyquotes.service;

import com.example.dailyquotes.domain.Quote;
import com.example.dailyquotes.domain.QuoteOrigin;
import com.example.dailyquotes.dto.ImportResultResponse;
import com.example.dailyquotes.mapper.QuoteMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 内置中文名人名言导入服务。
 *
 * <p>从类路径资源 {@code data/chinese-quotes.json} 读取一份精选的中文名人名言（离线、无网络依赖），
 * 按「内容 + 作者」指纹去重后写入 {@code quote} 表，来源标记为 {@code CN_SEED}。
 * 导入是幂等的——已存在的名言会被跳过，因此可安全地在每次启动时执行或在管理接口手动触发，
 * 不会因重复写入而报错。</p>
 */
@Service
public class ChineseQuoteImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChineseQuoteImporter.class);

    private final QuoteMapper quoteMapper;
    private final ObjectMapper objectMapper;
    private final String resourcePath;

    public ChineseQuoteImporter(QuoteMapper quoteMapper,
                               ObjectMapper objectMapper,
                               @Value("${app.chinese-quotes.resource:data/chinese-quotes.json}") String resourcePath) {
        this.quoteMapper = quoteMapper;
        this.objectMapper = objectMapper;
        this.resourcePath = resourcePath;
    }

    /**
     * 导入内置中文名人名言（幂等，不限条数）。
     *
     * @return 导入结果（新增、跳过、资源总数）
     */
    @Transactional
    public ImportResultResponse importBuiltIn() {
        return importBuiltIn(Integer.MAX_VALUE);
    }

    /**
     * 导入内置中文名人名言（幂等，最多新增 {@code maxNew} 条）。
     *
     * @param maxNew 本次最多新增条数（用于「中文优先」阶段按当日剩余配额封顶）
     * @return 导入结果（新增、跳过、资源总数）
     */
    @Transactional
    public ImportResultResponse importBuiltIn(int maxNew) {
        List<ChineseQuoteItem> items = readItems();
        int inserted = 0;
        int skipped = 0;
        for (ChineseQuoteItem item : items) {
            if (inserted >= maxNew) {
                break;
            }
            if (item.content == null || item.content.isBlank()) {
                continue;
            }
            String author = (item.author == null || item.author.isBlank()) ? "佚名" : item.author;
            String source = (item.source == null || item.source.isBlank()) ? "中文名言集" : item.source;
            String fingerprint = Quote.fingerprintOf(item.content, author);
            if (quoteMapper.existsByFingerprint(fingerprint)) {
                skipped++;
                continue;
            }
            Quote quote = new Quote();
            quote.setContent(item.content);
            quote.setAuthor(author);
            quote.setSource(source);
            quote.setPopularity(85);
            quote.setFeatured(false);
            quote.setOrigin(QuoteOrigin.CN_SEED.name());
            quote.setLang("zh");
            quote.setFingerprint(fingerprint);
            quoteMapper.insert(quote);
            inserted++;
        }
        LOGGER.info("[中文导入] 内置中文名人名言导入完成：新增 {} 条，跳过重复 {} 条，资源共 {} 条",
                inserted, skipped, items.size());
        return new ImportResultResponse(inserted, skipped, items.size());
    }

    private List<ChineseQuoteItem> readItems() {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            try (InputStream inputStream = resource.getInputStream()) {
                return objectMapper.readValue(inputStream, new TypeReference<List<ChineseQuoteItem>>() {
                });
            }
        } catch (IOException exception) {
            LOGGER.error("[中文导入] 读取内置中文名言资源失败：{}", exception.getMessage());
            return List.of();
        }
    }

    /** 资源条目结构，对应 {@code data/chinese-quotes.json} 中的对象。 */
    private static class ChineseQuoteItem {
        public String content;
        public String author;
        public String source;
    }
}
