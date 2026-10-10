package com.example.dailyquotes.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 中文名人名言「多源」在线采集客户端。
 *
 * <p>为「采集时优先采集中文」提供在线中文名言来源，按**优先级**分两组调用：
 * <ul>
 *   <li><b>① 优先：已配置 key 的数据源</b>（更稳定、额度更高、名言分类更丰富）——
 *       ALAPI 名人名言、天聚数行 名言警句、ShowAPI 名言警句、接口盒子 一言；</li>
 *   <li><b>② 兜底：公开（免 key）数据源</b>——一言 hitokoto（哲学/文学/诗词）、清和 API（古诗词名句）、客官 API。</li>
 * </ul>
 * 需 key 的源仅在配置了对应 token 时启用；每轮先轮询 key 源，额度/条数不足时再用公开源补足。
 * 各源响应结构差异较大（对象/数组、字段名不一、存在嵌套容器），因此这里按源分别解析，
 * 并用「先到先得、去重累积」的方式在限定轮数内尽量凑满目标条数。任一源失败（不可达/限流/结构变化）
 * 都会跳过而不影响其它源，保证整体采集的健壮性。</p>
 */
@Component
public class ChineseQuoteClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChineseQuoteClient.class);

    private final RestTemplate restTemplate;

    private final boolean onlineEnabled;
    private final int onlineLimit;
    private final int maxRounds;

    private final boolean hitokotoEnabled;
    private final boolean qhsouEnabled;
    private final boolean keguanEnabled;

    private final String alapiToken;
    private final String tianapiKey;
    private final String showapiKey;
    private final String apihzId;
    private final String apihzKey;

    public ChineseQuoteClient(RestTemplateBuilder builder,
                              @Value("${app.chinese-quotes.online-enabled:true}") boolean onlineEnabled,
                              @Value("${app.chinese-quotes.online-limit:20}") int onlineLimit,
                              @Value("${app.chinese-quotes.online-max-rounds:8}") int maxRounds,
                              @Value("${app.chinese-quotes.hitokoto-enabled:true}") boolean hitokotoEnabled,
                              @Value("${app.chinese-quotes.qhsou-enabled:true}") boolean qhsouEnabled,
                              @Value("${app.chinese-quotes.keguan-enabled:true}") boolean keguanEnabled,
                              @Value("${app.chinese-quotes.alapi-token:}") String alapiToken,
                              @Value("${app.chinese-quotes.tianapi-key:}") String tianapiKey,
                              @Value("${app.chinese-quotes.showapi-key:}") String showapiKey,
                              @Value("${app.chinese-quotes.apihz-id:}") String apihzId,
                              @Value("${app.chinese-quotes.apihz-key:}") String apihzKey) {
        this.restTemplate = builder.build();
        this.onlineEnabled = onlineEnabled;
        this.onlineLimit = onlineLimit;
        this.maxRounds = Math.max(1, maxRounds);
        this.hitokotoEnabled = hitokotoEnabled;
        this.qhsouEnabled = qhsouEnabled;
        this.keguanEnabled = keguanEnabled;
        this.alapiToken = alapiToken;
        this.tianapiKey = tianapiKey;
        this.showapiKey = showapiKey;
        this.apihzId = apihzId;
        this.apihzKey = apihzKey;
    }

    /**
     * 采集在线中文名言（多源、去重）。
     *
     * @param limit 本次最多返回条数
     * @return 中文名言列表（content/author/source 已填充，author 为空时回退为「佚名」）
     */
    public List<RemoteQuote> fetchChineseQuotes(int limit) {
        List<RemoteQuote> result = new ArrayList<>();
        if (!onlineEnabled || limit <= 0) {
            return result;
        }
        Set<String> seen = new HashSet<>();
        for (int round = 0; round < maxRounds && result.size() < limit; round++) {
            int before = result.size();
            // ① 优先调用已配置 key 的数据源（更稳定、额度更高、名言分类更丰富）
            if (hasText(alapiToken)) {
                collect(result, seen, limit, this::fetchAlapi);
            }
            if (hasText(tianapiKey)) {
                collect(result, seen, limit, this::fetchTianapi);
            }
            if (hasText(showapiKey)) {
                collect(result, seen, limit, this::fetchShowApi);
            }
            if (hasText(apihzId) && hasText(apihzKey)) {
                collect(result, seen, limit, this::fetchApiHz);
            }
            // ② key 源不足时，再用公开（免 key）数据源补足
            if (hitokotoEnabled) {
                collect(result, seen, limit, this::fetchHitokoto);
            }
            if (qhsouEnabled) {
                collect(result, seen, limit, this::fetchQhsouPoetry);
            }
            if (keguanEnabled) {
                collect(result, seen, limit, this::fetchKeguan);
            }
            // 一整轮没有任何新内容（多源均不可达/已重复）则提前结束，避免空转
            if (result.size() == before) {
                break;
            }
        }
        LOGGER.info("[中文在线] 采集到中文名人名言 {} 条（目标 {}，轮数上限 {}）", result.size(), limit, maxRounds);
        return result;
    }

    /** 单个源的一次拉取，返回解析后的名言（可能为空）。 */
    private interface SourceCall {
        List<RemoteQuote> call();
    }

    private void collect(List<RemoteQuote> result, Set<String> seen, int limit, SourceCall call) {
        if (result.size() >= limit) {
            return;
        }
        List<RemoteQuote> fetched;
        try {
            fetched = call.call();
        } catch (Exception exception) {
            LOGGER.warn("[中文在线] 数据源拉取失败：{}", exception.getMessage());
            return;
        }
        for (RemoteQuote quote : fetched) {
            if (result.size() >= limit) {
                break;
            }
            String content = quote.content() == null ? "" : quote.content().strip();
            String author = quote.author() == null || quote.author().isBlank() ? "佚名" : quote.author().strip();
            if (content.isEmpty()) {
                continue;
            }
            String key = content + "||" + author;
            if (!seen.add(key)) {
                continue;
            }
            result.add(new RemoteQuote(content, author, quote.source()));
        }
    }

    // ---------------------------------------------------------------- 免 key 源

    /** 一言 hitokoto：c=k 哲学（名言）；返回 {hitokoto, from, from_who}。 */
    private List<RemoteQuote> fetchHitokoto() {
        JsonNode node = get("https://v1.hitokoto.cn/?c=k&max_number=1");
        List<RemoteQuote> list = new ArrayList<>();
        // 单对象或数组都兼容
        if (node != null) {
            if (node.isArray()) {
                node.forEach(item -> addHitokoto(list, item));
            } else {
                addHitokoto(list, node);
            }
        }
        return list;
    }

    private void addHitokoto(List<RemoteQuote> list, JsonNode item) {
        String content = text(item, "hitokoto");
        String author = text(item, "from_who");
        String from = text(item, "from");
        if (hasText(content)) {
            if (!hasText(author)) {
                author = hasText(from) ? from : "佚名";
            }
            list.add(new RemoteQuote(content, author, hasText(from) ? from : "一言 hitokoto"));
        }
    }

    /** 清和 API 古诗词名句：{data:{content, author, works}}。 */
    private List<RemoteQuote> fetchQhsouPoetry() {
        JsonNode node = get("https://api.qhsou.com/api/gsc.php?encode=json");
        List<RemoteQuote> list = new ArrayList<>();
        if (node != null) {
            JsonNode data = node.path("data");
            String content = text(data, "content");
            if (hasText(content)) {
                String author = text(data, "author");
                String works = text(data, "works");
                list.add(new RemoteQuote(content, hasText(author) ? author : "佚名",
                        hasText(works) ? works : "清和API·古诗词"));
            }
        }
        return list;
    }

    /** 清和 API 随机一言：{content}（无作者，质量一般，作为兜底）。 */
    private List<RemoteQuote> fetchQhsouYiyan() {
        JsonNode node = get("https://api.qhsou.com/api/yy.php?encode=json&code=15");
        List<RemoteQuote> list = new ArrayList<>();
        if (node != null) {
            String content = text(node, "content");
            if (hasText(content)) {
                list.add(new RemoteQuote(content, "佚名", "清和API·一言"));
            }
        }
        return list;
    }

    /** 客官 API 随机一言：{errno, data:[{content, author}]}（当前可能不可达，失败自动跳过）。 */
    private List<RemoteQuote> fetchKeguan() {
        JsonNode node = get("https://api.keguan.org.cn/api/yy.php?encode=json");
        List<RemoteQuote> list = new ArrayList<>();
        if (node != null) {
            JsonNode data = node.path("data");
            JsonNode first = data.isArray() && data.size() > 0 ? data.get(0) : data;
            String content = text(first, "content", "text", "title");
            if (hasText(content)) {
                String author = text(first, "author", "from");
                list.add(new RemoteQuote(content, hasText(author) ? author : "佚名", "客官API·一言"));
            }
        }
        return list;
    }

    // ---------------------------------------------------------------- 需 key 源

    /** ALAPI 名人名言：{data:{content, author} 或 data:[...]}，typeid 可配置分类。 */
    private List<RemoteQuote> fetchAlapi() {
        JsonNode node = get("https://v3.alapi.cn/api/mingyan?typeid=1&token=" + alapiToken
                + "&format=json");
        List<RemoteQuote> list = new ArrayList<>();
        JsonNode data = node == null ? null : node.path("data");
        if (data != null) {
            JsonNode first = data.isArray() && data.size() > 0 ? data.get(0) : data;
            String content = text(first, "content");
            if (hasText(content)) {
                String author = text(first, "author");
                list.add(new RemoteQuote(content, hasText(author) ? author : "佚名", "ALAPI·名人名言"));
            }
        }
        return list;
    }

    /** 天聚数行 名言警句：{result:{list:[{content, mrname}]}}（实测结构，num 控制条数）。 */
    private List<RemoteQuote> fetchTianapi() {
        JsonNode node = get("https://apis.tianapi.com/dictum/index?key=" + tianapiKey + "&num=10");
        List<RemoteQuote> list = new ArrayList<>();
        JsonNode arr = node == null ? null : node.path("result").path("list");
        if (arr != null && arr.isArray()) {
            for (JsonNode item : arr) {
                String content = text(item, "content");
                if (hasText(content)) {
                    String author = text(item, "mrname");
                    list.add(new RemoteQuote(content, hasText(author) ? author : "佚名", "天聚数行·名言警句"));
                }
            }
        }
        return list;
    }

    /** ShowAPI 名言警句：{showapi_res_body:{data:[{Rcontent, Rtitle, Rauthor}]}}（需 api.showapi.com 域名）。 */
    private List<RemoteQuote> fetchShowApi() {
        JsonNode node = get("https://api.showapi.com/apiGateway/view/1839?key=" + showapiKey + "&page=1&num=10");
        List<RemoteQuote> list = new ArrayList<>();
        JsonNode arr = node == null ? null : node.path("showapi_res_body").path("data");
        if (arr != null && arr.isArray()) {
            for (JsonNode item : arr) {
                String content = text(item, "Rcontent", "Rtitle", "content");
                if (hasText(content)) {
                    String author = text(item, "Rauthor", "author");
                    list.add(new RemoteQuote(content, hasText(author) ? author : "佚名", "ShowAPI·名言警句"));
                }
            }
        }
        return list;
    }

    /** 接口盒子 一言：{data:{content, from}}（实测需同时提供 id 与 key，否则返回 code=400）。 */
    private List<RemoteQuote> fetchApiHz() {
        JsonNode node = get("https://cn.apihz.cn/api/yiyan/api.php?id=" + apihzId + "&key=" + apihzKey);
        List<RemoteQuote> list = new ArrayList<>();
        if (node != null) {
            JsonNode data = node.path("data");
            String content = text(data, "content");
            if (hasText(content)) {
                String from = text(data, "from");
                list.add(new RemoteQuote(content, "佚名", hasText(from) ? from : "接口盒子·一言"));
            }
        }
        return list;
    }

    // ---------------------------------------------------------------- 工具方法

    private JsonNode get(String url) {
        try {
            return restTemplate.getForObject(url, JsonNode.class);
        } catch (Exception exception) {
            throw new IllegalStateException("请求失败: " + url, exception);
        }
    }

    private static String text(JsonNode node, String... fields) {
        if (node == null) {
            return "";
        }
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isTextual() && !value.asText().isBlank()) {
                return value.asText().strip();
            }
        }
        return "";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
