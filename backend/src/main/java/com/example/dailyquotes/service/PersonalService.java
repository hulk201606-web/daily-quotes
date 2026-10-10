package com.example.dailyquotes.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.dailyquotes.domain.Quote;
import com.example.dailyquotes.domain.User;
import com.example.dailyquotes.domain.UserFavorite;
import com.example.dailyquotes.domain.UserQuoteTag;
import com.example.dailyquotes.domain.UserTag;
import com.example.dailyquotes.dto.PageResponse;
import com.example.dailyquotes.dto.QuoteResponse;
import com.example.dailyquotes.dto.UserTagResponse;
import com.example.dailyquotes.exception.ResourceNotFoundException;
import com.example.dailyquotes.mapper.QuoteMapper;
import com.example.dailyquotes.mapper.UserFavoriteMapper;
import com.example.dailyquotes.mapper.UserQuoteTagMapper;
import com.example.dailyquotes.mapper.UserTagMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 个人化数据服务：承载「按个人标签、个人喜好收录」的能力。
 *
 * <p>所有数据均以 {@link User#getId()} 为隔离边界，不同用户之间的标签、收藏与打标互不干扰：
 * <ul>
 *   <li>个人标签字典（{@code user_tag}）——用户自建的标签，如「治愈」「励志」；</li>
 *   <li>个人收藏（{@code user_favorite}）——把某条名言收进自己的收藏夹（个人喜好收录）；</li>
 *   <li>名言打标签（{@code user_quote_tag}）——把个人标签贴到某条名言上，实现「按标签归类收录」。</li>
 * </ul>
 * 创建类操作均设计为幂等：重复收藏 / 重复打标 / 重名标签会直接返回已有记录，避免唯一键冲突报错。</p>
 */
@Service
public class PersonalService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersonalService.class);

    private final UserTagMapper userTagMapper;
    private final UserFavoriteMapper userFavoriteMapper;
    private final UserQuoteTagMapper userQuoteTagMapper;
    private final QuoteMapper quoteMapper;
    private final QuoteService quoteService;

    public PersonalService(UserTagMapper userTagMapper,
                           UserFavoriteMapper userFavoriteMapper,
                           UserQuoteTagMapper userQuoteTagMapper,
                           QuoteMapper quoteMapper,
                           QuoteService quoteService) {
        this.userTagMapper = userTagMapper;
        this.userFavoriteMapper = userFavoriteMapper;
        this.userQuoteTagMapper = userQuoteTagMapper;
        this.quoteMapper = quoteMapper;
        this.quoteService = quoteService;
    }

    /** 列举当前用户的全部个人标签。 */
    @Transactional(readOnly = true)
    public List<UserTagResponse> listMyTags(User user) {
        LOGGER.info("[DB查询] 列举用户 {} 的个人标签", user.getId());
        return userTagMapper.selectList(new QueryWrapper<UserTag>().eq("user_id", user.getId()).orderByAsc("id"))
                .stream().map(UserTagResponse::from).collect(Collectors.toList());
    }

    /**
     * 新建个人标签（幂等）：若同名标签已存在则直接返回已有记录，不报错。
     *
     * @return 新建或已存在的标签响应
     */
    @Transactional
    public UserTagResponse createTag(User user, String name) {
        LOGGER.info("[DB新增] 用户 {} 创建个人标签 name={}", user.getId(), name);
        UserTag existing = userTagMapper.selectOne(
                new QueryWrapper<UserTag>().eq("user_id", user.getId()).eq("name", name));
        if (existing != null) {
            return UserTagResponse.from(existing);
        }
        UserTag tag = new UserTag(user.getId(), name);
        userTagMapper.insert(tag);
        return UserTagResponse.from(tag);
    }

    /** 删除个人标签，并同时清理该标签在所有名言上的关联（避免孤儿数据）。 */
    @Transactional
    public void deleteTag(User user, Long tagId) {
        LOGGER.info("[DB删除] 用户 {} 删除个人标签 id={}", user.getId(), tagId);
        UserTag tag = userTagMapper.selectById(tagId);
        if (tag == null || !tag.getUserId().equals(user.getId())) {
            throw new ResourceNotFoundException("未找到属于当前用户的标签 id=" + tagId);
        }
        userQuoteTagMapper.delete(new QueryWrapper<UserQuoteTag>()
                .eq("user_id", user.getId()).eq("tag_id", tagId));
        userTagMapper.deleteById(tagId);
    }

    /** 列举当前用户打在某条名言上的个人标签。 */
    @Transactional(readOnly = true)
    public List<UserTagResponse> listQuoteTags(User user, Long quoteId) {
        LOGGER.info("[DB查询] 用户 {} 在名言 {} 上的个人标签", user.getId(), quoteId);
        List<Long> tagIds = userQuoteTagMapper.selectList(
                        new QueryWrapper<UserQuoteTag>().eq("user_id", user.getId()).eq("quote_id", quoteId))
                .stream().map(UserQuoteTag::getTagId).collect(Collectors.toList());
        if (tagIds.isEmpty()) {
            return List.of();
        }
        return userTagMapper.selectList(new QueryWrapper<UserTag>().in("id", tagIds))
                .stream().map(UserTagResponse::from).collect(Collectors.toList());
    }

    /**
     * 给某条名言打上个人标签（幂等）：重复打标直接返回，不报错。
     *
     * @throws ResourceNotFoundException 当名言不存在，或标签不属于当前用户时
     */
    @Transactional
    public void addTagToQuote(User user, Long quoteId, Long tagId) {
        LOGGER.info("[DB新增] 用户 {} 给名言 {} 打标签 {}", user.getId(), quoteId, tagId);
        assertQuoteExists(quoteId);
        UserTag tag = userTagMapper.selectById(tagId);
        if (tag == null || !tag.getUserId().equals(user.getId())) {
            throw new ResourceNotFoundException("未找到属于当前用户的标签 id=" + tagId);
        }
        long count = userQuoteTagMapper.selectCount(new QueryWrapper<UserQuoteTag>()
                .eq("user_id", user.getId()).eq("quote_id", quoteId).eq("tag_id", tagId));
        if (count == 0) {
            userQuoteTagMapper.insert(new UserQuoteTag(user.getId(), quoteId, tagId));
        }
    }

    /** 移除某条名言上的某个个人标签。 */
    @Transactional
    public void removeTagFromQuote(User user, Long quoteId, Long tagId) {
        LOGGER.info("[DB删除] 用户 {} 移除名言 {} 的标签 {}", user.getId(), quoteId, tagId);
        userQuoteTagMapper.delete(new QueryWrapper<UserQuoteTag>()
                .eq("user_id", user.getId()).eq("quote_id", quoteId).eq("tag_id", tagId));
    }

    /**
     * 收藏某条名言（幂等）：重复收藏直接返回，不报错。
     *
     * @throws ResourceNotFoundException 当名言不存在时
     */
    @Transactional
    public void addFavorite(User user, Long quoteId) {
        LOGGER.info("[DB新增] 用户 {} 收藏名言 {}", user.getId(), quoteId);
        assertQuoteExists(quoteId);
        long count = userFavoriteMapper.selectCount(new QueryWrapper<UserFavorite>()
                .eq("user_id", user.getId()).eq("quote_id", quoteId));
        if (count == 0) {
            userFavoriteMapper.insert(new UserFavorite(user.getId(), quoteId));
        }
    }

    /** 取消收藏某条名言。 */
    @Transactional
    public void removeFavorite(User user, Long quoteId) {
        LOGGER.info("[DB删除] 用户 {} 取消收藏名言 {}", user.getId(), quoteId);
        userFavoriteMapper.delete(new QueryWrapper<UserFavorite>()
                .eq("user_id", user.getId()).eq("quote_id", quoteId));
    }

    /** 分页列举当前用户的收藏（个人喜好收录），并附带其个人标签与收藏标记。 */
    @Transactional(readOnly = true)
    public PageResponse<QuoteResponse> listFavorites(User user, int page, int size) {
        LOGGER.info("[DB查询] 用户 {} 的收藏列表 page={}, size={}", user.getId(), page, size);
        long total = userFavoriteMapper.countByUser(user.getId());
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        long offset = (long) safePage * safeSize;
        List<Quote> quotes = userFavoriteMapper.selectFavoriteQuotes(user.getId(), safeSize, offset);
        return PageResponse.of(quoteService.toResponses(quotes, user), total, safePage + 1, safeSize);
    }

    /** 分页列举当前用户在某标签下收录的名言（按个人喜好标签归类浏览）。 */
    @Transactional(readOnly = true)
    public PageResponse<QuoteResponse> listQuotesByTag(User user, Long tagId, int page, int size) {
        LOGGER.info("[DB查询] 用户 {} 标签 {} 下的名言 page={}, size={}", user.getId(), tagId, page, size);
        UserTag tag = userTagMapper.selectById(tagId);
        if (tag == null || !tag.getUserId().equals(user.getId())) {
            throw new ResourceNotFoundException("未找到属于当前用户的标签 id=" + tagId);
        }
        long total = userQuoteTagMapper.countByUserAndTag(user.getId(), tagId);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        long offset = (long) safePage * safeSize;
        List<Quote> quotes = userQuoteTagMapper.selectQuotesByTag(user.getId(), tagId, safeSize, offset);
        return PageResponse.of(quoteService.toResponses(quotes, user), total, safePage + 1, safeSize);
    }

    /** 断言名言存在，不存在则抛出资源未找到异常。 */
    private void assertQuoteExists(Long quoteId) {
        if (quoteMapper.selectById(quoteId) == null) {
            throw new ResourceNotFoundException("未找到 id=" + quoteId + " 的名言");
        }
    }
}
