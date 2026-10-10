package com.example.dailyquotes.controller;

import com.example.dailyquotes.config.CurrentUser;
import com.example.dailyquotes.domain.User;
import com.example.dailyquotes.dto.FetchResultResponse;
import com.example.dailyquotes.dto.ImportResultResponse;
import com.example.dailyquotes.dto.PageResponse;
import com.example.dailyquotes.dto.QuoteRequest;
import com.example.dailyquotes.dto.QuoteResponse;
import com.example.dailyquotes.dto.QuoteStatsResponse;
import com.example.dailyquotes.dto.TranslateResultResponse;
import com.example.dailyquotes.security.AuthUtils;
import com.example.dailyquotes.service.ChineseQuoteImporter;
import com.example.dailyquotes.service.QuoteFetchService;
import com.example.dailyquotes.service.QuoteService;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 名言 REST 接口。
 *
 * <p>对外暴露名言的查询、统计、维护（新增/编辑/删除）以及手动触发每日采集等能力，
 * 入参经 Jakarta Bean Validation 校验；所有写操作落到 {@link QuoteService} / {@link QuoteFetchService}。
 * 统一异常处理由 {@code GlobalExceptionHandler} 负责，返回结构化错误响应。</p>
 *
 * <p>权限模型（需求：除管理员外其他人只能查看、不能修改删除）：
 * 查询类接口（list/get/stats）对所有已识别或无身份用户开放；
 * 写类接口（create/update/delete/fetch）前置 {@link AuthUtils#requireAdmin(User)} 校验，
 * 仅管理员可通过，其余用户调用将收到 403。</p>
 */
@Validated
@RestController
@RequestMapping("/api/quotes")
public class QuoteController {

    private final QuoteService quoteService;
    private final QuoteFetchService quoteFetchService;
    private final ChineseQuoteImporter chineseQuoteImporter;

    public QuoteController(QuoteService quoteService,
                           QuoteFetchService quoteFetchService,
                           ChineseQuoteImporter chineseQuoteImporter) {
        this.quoteService = quoteService;
        this.quoteFetchService = quoteFetchService;
        this.chineseQuoteImporter = chineseQuoteImporter;
    }

    /** 分页/搜索/排序查询名言，支持关键词、出处/语种筛选与多维度排序；匿名或任意用户均可访问。 */
    @GetMapping
    public PageResponse<QuoteResponse> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String source,
            @RequestParam(defaultValue = "") String lang,
            @RequestParam(defaultValue = "popularity") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size,
            @CurrentUser User user) {
        return quoteService.findQuotes(keyword, source, lang, sortBy, direction, page, size, user);
    }

    /** 按主键查询单条名言；若携带用户身份则返回其个人标签与收藏态。 */
    @GetMapping("/{id}")
    public QuoteResponse get(@PathVariable Long id, @CurrentUser User user) {
        return quoteService.findById(id, user);
    }

    /** 返回名言统计概览（总数、来源分布、今日采集与出处列表）；只读接口，所有用户可访问。 */
    @GetMapping("/stats")
    public QuoteStatsResponse stats() {
        return quoteService.stats();
    }

    /** 人工新增一条名言（来源标记为 MANUAL）；仅管理员可操作，返回 201 与新建记录。 */
    @PostMapping
    public ResponseEntity<QuoteResponse> create(@CurrentUser User user,
                                                @Valid @RequestBody QuoteRequest request) {
        AuthUtils.requireAdmin(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(quoteService.create(request));
    }

    /** 编辑指定 id 的名言，覆盖内容、作者、出处、知名度与精选状态；仅管理员可操作。 */
    @PutMapping("/{id}")
    public QuoteResponse update(@CurrentUser User user,
                                @PathVariable Long id,
                                @Valid @RequestBody QuoteRequest request) {
        AuthUtils.requireAdmin(user);
        return quoteService.update(id, request);
    }

    /** 删除指定 id 的名言，成功返回 204；仅管理员可操作。 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUser User user, @PathVariable Long id) {
        AuthUtils.requireAdmin(user);
        quoteService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** 手动触发一次每日采集（标记为 MANUAL）；仅管理员可操作，返回采集结果摘要。 */
    @PostMapping("/fetch")
    public FetchResultResponse fetch(@CurrentUser User user) {
        AuthUtils.requireAdmin(user);
        return quoteFetchService.fetchNow("MANUAL");
    }

    /**
     * 导入内置中文名人名言（幂等，已存在则跳过）；仅管理员可操作。
     * 数据来自类路径资源 {@code data/chinese-quotes.json}，离线、无网络依赖。
     */
    @PostMapping("/import-chinese")
    public ImportResultResponse importChinese(@CurrentUser User user) {
        AuthUtils.requireAdmin(user);
        return chineseQuoteImporter.importBuiltIn();
    }

    /**
     * 将历史外文采集的名言翻译为中文（content 改中文、originalContent 留存原文）；仅管理员可操作。
     * 逐条跳过失败项并小间隔节流，尽量在免费额度内多翻；连续失败达阈值判定额度耗尽即结束本轮，
     * 剩余条目可反复触发本接口续做。
     */
    @PostMapping("/translate-pending")
    public TranslateResultResponse translatePending(@CurrentUser User user) {
        AuthUtils.requireAdmin(user);
        return quoteFetchService.translatePendingQuotes();
    }
}
