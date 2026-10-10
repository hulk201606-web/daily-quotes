package com.example.dailyquotes.controller;

import com.example.dailyquotes.config.CurrentUser;
import com.example.dailyquotes.domain.User;
import com.example.dailyquotes.dto.PageResponse;
import com.example.dailyquotes.dto.QuoteResponse;
import com.example.dailyquotes.dto.QuoteTagRequest;
import com.example.dailyquotes.dto.UserTagRequest;
import com.example.dailyquotes.dto.UserTagResponse;
import com.example.dailyquotes.security.AuthUtils;
import com.example.dailyquotes.service.PersonalService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人化数据接口（按个人标签 / 个人喜好收录）。
 *
 * <p>所有接口均要求已识别当前用户（请求头 {@code X-User-Id}），数据严格隔离在各自用户名下：
 * <ul>
 *   <li>个人标签字典：{@code GET/POST /api/personal/tags}、{@code DELETE /api/personal/tags/{tagId}}；</li>
 *   <li>个人收藏（喜好收录）：{@code GET /api/personal/favorites}、收藏/取消 {@code /api/personal/quotes/{quoteId}/favorite}；</li>
 *   <li>名言打标签（归类收录）：{@code GET/POST /api/personal/quotes/{quoteId}/tags}、移除 {@code .../tags/{tagId}}；</li>
 *   <li>按标签浏览收录：{@code GET /api/personal/quotes?tagId=...}。</li>
 * </ul>
 * 这些能力面向所有已登录用户（含管理员），与「仅管理员可改公共名言」的权限互不冲突。</p>
 */
@RestController
@RequestMapping("/api/personal")
public class PersonalController {

    private final PersonalService personalService;

    public PersonalController(PersonalService personalService) {
        this.personalService = personalService;
    }

    /** 列举当前用户的全部个人标签。 */
    @GetMapping("/tags")
    public List<UserTagResponse> listTags(@CurrentUser User user) {
        AuthUtils.requireUser(user);
        return personalService.listMyTags(user);
    }

    /** 新建个人标签（重名时返回已有记录）。 */
    @PostMapping("/tags")
    public ResponseEntity<UserTagResponse> createTag(@CurrentUser User user,
                                                     @Valid @RequestBody UserTagRequest request) {
        AuthUtils.requireUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(personalService.createTag(user, request.name()));
    }

    /** 删除个人标签（同时清理该标签在所有名言上的关联）。 */
    @DeleteMapping("/tags/{tagId}")
    public ResponseEntity<Void> deleteTag(@CurrentUser User user, @PathVariable Long tagId) {
        AuthUtils.requireUser(user);
        personalService.deleteTag(user, tagId);
        return ResponseEntity.noContent().build();
    }

    /** 列举当前用户的收藏（个人喜好收录），分页返回。 */
    @GetMapping("/favorites")
    public PageResponse<QuoteResponse> favorites(@CurrentUser User user,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "12") int size) {
        AuthUtils.requireUser(user);
        return personalService.listFavorites(user, page, size);
    }

    /** 收藏某条名言（个人喜好收录）。 */
    @PostMapping("/quotes/{quoteId}/favorite")
    public ResponseEntity<Void> addFavorite(@CurrentUser User user, @PathVariable Long quoteId) {
        AuthUtils.requireUser(user);
        personalService.addFavorite(user, quoteId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** 取消收藏某条名言。 */
    @DeleteMapping("/quotes/{quoteId}/favorite")
    public ResponseEntity<Void> removeFavorite(@CurrentUser User user, @PathVariable Long quoteId) {
        AuthUtils.requireUser(user);
        personalService.removeFavorite(user, quoteId);
        return ResponseEntity.noContent().build();
    }

    /** 列举当前用户打在某条名言上的个人标签。 */
    @GetMapping("/quotes/{quoteId}/tags")
    public List<UserTagResponse> quoteTags(@CurrentUser User user, @PathVariable Long quoteId) {
        AuthUtils.requireUser(user);
        return personalService.listQuoteTags(user, quoteId);
    }

    /** 给某条名言打上个人标签（归类收录）。 */
    @PostMapping("/quotes/{quoteId}/tags")
    public ResponseEntity<Void> addTag(@CurrentUser User user,
                                       @PathVariable Long quoteId,
                                       @Valid @RequestBody QuoteTagRequest request) {
        AuthUtils.requireUser(user);
        personalService.addTagToQuote(user, quoteId, request.tagId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** 移除某条名言上的某个个人标签。 */
    @DeleteMapping("/quotes/{quoteId}/tags/{tagId}")
    public ResponseEntity<Void> removeTag(@CurrentUser User user,
                                          @PathVariable Long quoteId,
                                          @PathVariable Long tagId) {
        AuthUtils.requireUser(user);
        personalService.removeTagFromQuote(user, quoteId, tagId);
        return ResponseEntity.noContent().build();
    }

    /** 按个人标签分页浏览收录的名言。 */
    @GetMapping("/quotes")
    public PageResponse<QuoteResponse> quotesByTag(@CurrentUser User user,
                                                   @RequestParam Long tagId,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "12") int size) {
        AuthUtils.requireUser(user);
        return personalService.listQuotesByTag(user, tagId, page, size);
    }
}
