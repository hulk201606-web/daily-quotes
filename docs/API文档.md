# 每日名言（Daily Quotes）API 文档

- **Base URL**：`http://localhost:8080/api/quotes`
- **Content-Type**：`application/json; charset=utf-8`
- **字符编码**：UTF-8（Windows 环境启动时请加 `-Dfile.encoding=UTF-8`）

## 通用说明

- 分页响应统一结构：

```json
{
  "content": [ "..." ],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5,
  "first": true,
  "last": false
}
```

- 错误响应统一结构：

```json
{
  "error": "DUPLICATE_QUOTE",
  "message": "该名言（内容 + 作者）已存在，请勿重复添加"
}
```

---

## 0. 身份识别与权限模型（新增）

系统采用 **轻量身份标识**：请求头 `X-User-Id` 标明当前登录用户（整数 id）。
系统无独立登录中心，前端通过「切换用户」模拟登录，所有写操作与个人隐私数据均以此头为准。

- **角色（`role`）**
  - `ADMIN`（管理员）：可对公共名言进行 **新增 / 编辑 / 删除 / 采集**。
  - `USER`（普通用户）：**仅可浏览**公共名言，并维护 **属于自己的** 个人标签与收藏。
- **公共名言写接口**（`POST/PUT/DELETE /api/quotes`、`POST /api/quotes/fetch`）要求 `ADMIN`；
  查询类接口（`list/get/stats`）对所有人开放。
- **个人数据接口**（`/api/personal/**`）面向所有已识别用户（含管理员），数据以 `user_id` 严格隔离，互不可见。
- 未携带 `X-User-Id` 或用户不存在 → **401**；权限不足（如非管理员改公共名言）→ **403**。

| 状态码 | 含义 | 典型场景 |
|--------|------|----------|
| 401 | 未授权（未携带有效用户） | 访问个人标签/收藏但未带 `X-User-Id` |
| 403 | 禁止访问（权限不足） | 非管理员调用 `POST /api/quotes` |
| 404 | 资源不存在 | 指定 id 的名言/标签不存在 |
| 409 | 数据冲突（唯一键/重复） | 录入重复名言 |
| 500 | 服务异常 | 未预期错误（含堆栈日志） |

> **联调提示**：所有 `/api/personal` 与个人化字段相关请求都必须带 `X-User-Id` 头；
> 不带该头时查询名言仍成功，但不会返回 `myTags`/`favorited`（见第 13 节）。

---

## 1. 分页查询名言

`GET /api/quotes`

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| keyword | String | "" | 关键词，模糊匹配「内容」或「作者」 |
| source | String | "" | 出处精确筛选 |
| lang | String | "" | 语种筛选：`zh`=中文 / `en`=外文（外文经翻译后 content 为中文、originalContent 为原文） |
| sortBy | String | popularity | 排序字段：`popularity`（知名度）/ `source`（出处）/ `author`（作者）/ `latest`（录入时间）/ `fetched`（采集时间，同一采集日内按录入时间二次排序，配合 `direction=desc` 让采集当日最新数据排在最前） |
| direction | String | desc | 排序方向：`asc` / `desc` |
| page | int | 0 | 页码（从 0 开始） |
| size | int | 20 | 每页条数（1-100，前端典藏默认 20） |

> **前端展示约定**：典藏列表默认请求 `size=20`、`sortBy=popularity`、`direction=desc`（即每页 20 条、按知名度降序），并保留分页以便浏览全部数据；用户可在界面切换为按出处 / 作者 / 收录时间排序，或切换升序。

**响应示例**

```json
{
  "content": [
    {
      "id": 3,
      "content": "纸上得来终觉浅，绝知此事要躬行。",
      "author": "陆游",
      "source": "冬夜读书示子聿",
      "popularity": 94,
      "featured": true,
      "origin": "SEED",
      "lastFetchedDate": null,
      "lang": "zh",
      "originalContent": null,
      "createdAt": "2026-10-03T09:47:02",
      "updatedAt": "2026-10-03T09:47:02"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 20,
  "totalPages": 1,
  "first": true,
  "last": false
}
```

> **新增响应字段（中文 / 翻译相关）**
> | 字段 | 类型 | 说明 |
> |------|------|------|
> | `lang` | String | 内容语种：`zh`=中文 / `en`=外文（外文经翻译后 `content` 为中文、`originalContent` 留存原文） |
> | `originalContent` | String\|null | 外文采集翻译前的原文；中文名言为 `null` |
> | `origin` | String | 来源：`SEED`（内置示例）/ `INTERNET`（外文采集，已翻译为中文）/ `MANUAL`（人工维护）/ `CN_SEED`（内置中文精选集）/ `CN_INTERNET`（在线中文名人名言） |

---

## 2. 根据 ID 查询单条名言

`GET /api/quotes/{id}`

**成功 200**

```json
{
  "id": 3,
  "content": "纸上得来终觉浅，绝知此事要躬行。",
  "author": "陆游",
  "source": "冬夜读书示子聿",
  "popularity": 94,
  "featured": true,
  "origin": "SEED",
  "lastFetchedDate": null,
  "lang": "zh",
  "originalContent": null,
  "createdAt": "2026-10-03T09:47:02",
  "updatedAt": "2026-10-03T09:47:02"
}
```

**失败 404**：`{ "error": "NOT_FOUND", "message": "未找到 id=999 的名言" }`

---

## 3. 统计信息

`GET /api/quotes/stats`

**响应示例**

```json
{
  "total": 164,
  "internetTotal": 100,
  "manualTotal": 0,
  "chineseSeedTotal": 54,
  "fetchedToday": 100,
  "sourceCount": 57,
  "sources": ["中庸", "冬夜读书示子聿", "劝学", "..."]
}
```

| 字段 | 说明 |
|------|------|
| total | 名言总数 |
| internetTotal | 互联网采集数量（已翻译为中文） |
| manualTotal | 人工维护数量 |
| chineseSeedTotal | 内置中文名人名言数量（来源 `CN_SEED`） |
| fetchedToday | 今日互联网采集数量 |
| sourceCount | 去重后的出处数量 |
| sources | 全部出处列表（用于筛选下拉） |

---

## 4. 新增名言（人工维护）

`POST /api/quotes`

**请求体**

```json
{
  "content": "学而不思则罔，思而不学则殆。",
  "author": "孔子",
  "source": "论语",
  "popularity": 97,
  "featured": true
}
```

| 字段 | 约束 |
|------|------|
| content | 必填，≤2000 字 |
| author | 必填，≤120 字 |
| source | 必填，≤180 字 |
| popularity | 必填，1-100 整数 |
| featured | 布尔，是否精选 |

**成功 201**

```json
{
  "id": 21,
  "content": "学而不思则罔，思而不学则殆。",
  "author": "孔子",
  "source": "论语",
  "popularity": 97,
  "featured": true,
  "origin": "MANUAL",
  "lastFetchedDate": null,
  "createdAt": "2026-10-03T10:00:00",
  "updatedAt": "2026-10-03T10:00:00"
}
```

**失败 400**（校验） / **409**（重复）：`{ "error": "DUPLICATE_QUOTE", "message": "该名言（内容 + 作者）已存在，请勿重复添加" }`

---

## 5. 编辑名言

`PUT /api/quotes/{id}`

请求体与新增一致，返回更新后的名言对象（同第 2 节结构）。

---

## 6. 删除名言

`DELETE /api/quotes/{id}`

**成功 204**：无响应体。

---

## 7. 手动触发每日采集

`POST /api/quotes/fetch`

从互联网公开名言库拉取不重复名言并入库，同时写入 `quote_fetch_log`。采集采用 **「中文优先、两阶段」** 策略：

1. **阶段一（中文优先）**——先从**在线中文名人名言接口**（见 7.3）采集，来源标记 `CN_INTERNET`；再导入**内置中文精选集**（`CN_SEED`）补足。让中文名言排在最前面采集；
2. **阶段二（外文兜底并翻译）**——若阶段一之后仍未凑满 `daily-limit`（默认 100）条，才从互联网采集其他语言名言，并**逐条翻译为中文**后补足差额（`content` 存中文、`originalContent` 存原文、`lang` 标记为 `en`）；若翻译服务不可用或命中配额，则降级保留原文（不阻断采集）。

**采集细节**
- 单次向数据源请求的条数受 `app.quote-fetch.max-page-size`（默认 10）约束：部分公开接口对单次拉取上限敏感（如 dummyjson 在 `limit>10` 时返回空数组），超出会导致整页为空、采集不到数据，因此代码会收敛到安全上限并多次拉取凑满配额。
- 默认每日目标 `app.quote-fetch.daily-limit` 条（默认 100），最大尝试次数 50 次；翻译开关由 `app.quote-fetch.translate-enabled`（默认 true）控制。

**成功 200**

```json
{
  "created": 100,
  "skipped": 1,
  "attempts": 11,
  "completedAt": "2026-10-03T12:54:52"
}
```

| 字段 | 说明 |
|------|------|
| created | 本次成功写入条数 |
| skipped | 因重复跳过的条数 |
| attempts | 采集尝试次数（最多 50 次） |
| completedAt | 完成时间 |

**失败 502**：`{ "error": "FETCH_FAILED", "message": "无法连接互联网名言服务，请稍后重试" }`

> 每日 06:30 会由调度器（`QuoteFetchScheduler`）以 `SCHEDULED` 类型自动执行相同逻辑；手动「立即采集」不受日限额约束，可随时补充（重复由指纹唯一索引兜底，不会写入重复数据）。

### 7.1 导入内置中文名人名言

`POST /api/quotes/import-chinese`（仅管理员）

从类路径资源 `data/chinese-quotes.json`（一份离线、无网络依赖的精选中文名人名言集）按「内容 + 作者」指纹去重后写入 `quote` 表，来源标记为 `CN_SEED`。**幂等**——已存在的名言会被跳过，可安全地在每次启动时自动执行或手动重复触发。

**成功 200**

```json
{ "inserted": 54, "skipped": 6, "total": 60 }
```

| 字段 | 说明 |
|------|------|
| inserted | 本次新增条数 |
| skipped | 因重复跳过的条数 |
| total | 资源文件中的总条数 |

### 7.2 外文名言回填翻译

`POST /api/quotes/translate-pending`（仅管理员）

将「外文采集但未翻译」的名言（来源 `INTERNET` 且 `original_content` 为空）翻译为中文：`content` 改写为中文、`originalContent` 留存英文原文、`lang` 标记为 `en`。用于翻译能力上线前、或免费翻译额度耗尽时残留的英文名言。

**处理策略（适配免费接口每日配额）**
- **逐条跳过**：单条翻译失败（配额/网络）不中断整批，跳过继续下一条；
- **小间隔节流**：每条之间 sleep `app.translation.interval-ms`（默认 350ms），避免瞬时突发浪费额度；
- **可反复续做**：成功翻译的记录 `original_content` 非空，再次触发自动跳过；连续失败达 `app.translation.max-consecutive-failures`（默认 20）即判定额度耗尽、结束本轮，剩余条目可**反复调用本接口续做**。

**成功 200**

```json
{ "translated": 174, "failed": 20, "remaining": 138 }
```

| 字段 | 说明 |
|------|------|
| translated | 本轮成功翻译条数 |
| failed | 本轮翻译失败（已跳过）条数 |
| remaining | 剩余待翻译条数（可再次触发续做） |

### 7.3 在线中文名人名言数据源（阶段一）

「中文优先」阶段会从下列**在线中文接口**采集名人名言（来源 `CN_INTERNET`）。调用优先级为：**先轮询已配置 key 的数据源**（额度更高、更稳定、分类更丰富），**额度/条数不足时再用公开（免 key）源补足**；单源失败自动跳过、不影响其它源。

| 数据源 | 地址 | 是否需 key | 备注 |
|--------|------|-----------|------|
| 一言 hitokoto | `https://v1.hitokoto.cn/?c=k` | 否 | 哲学/文学/诗词等，`hitokoto`+`from_who`+`from` |
| 清和 API（古诗词名句） | `https://api.qhsou.com/api/gsc.php?encode=json` | 否 | `data.content`+`data.author`+`data.works` |
| 客官 API | `https://api.keguan.org.cn/api/yy.php` | 否 | 部分环境可能不可达，失败自动跳过 |
| ALAPI 名人名言 | `https://v3.alapi.cn/api/mingyan?typeid=1&token=xxx` | 是（token） | 45 个分类，免费 100 次/天 |
| 天聚数行 名言警句 | `https://apis.tianapi.com/dictum/index?key=xxx` | 是（key） | 注册免费 |
| ShowAPI 名言警句 | `https://api.showapi.com/apiGateway/view/1839` | 是（key） | 可按标签/作者/主题筛选 |
| 接口盒子 一言 | `https://cn.apihz.cn/api/yiyan/api.php?id=xxx&key=xxx` | 是（数字ID **和** 秘钥key） | 12 万条语录；仅提供 id 会返回 400，需同时配置 `apihz-key` |

配置位于 `application.yml` 的 `app.chinese-quotes`：`online-limit`（每次在线补充上限）、`online-max-rounds`（轮数）、各源 `*-enabled` 开关，以及需 key 的源填入对应 token（留空即禁用）。

---

## 8. 用户接口

### 8.1 列举全部用户（脱敏）

`GET /api/users`

返回全部用户，供前端「切换身份 / 模拟登录」使用。**无需** `X-User-Id`。

**响应示例**

```json
[
  { "id": 1, "username": "admin", "displayName": "管理员", "role": "ADMIN" },
  { "id": 2, "username": "zhangsan", "displayName": "张三", "role": "USER" },
  { "id": 3, "username": "lisi", "displayName": "李四", "role": "USER" }
]
```

### 8.2 获取当前用户信息

`GET /api/users/me`

需携带 `X-User-Id` 头，否则返回 **401**。

**成功 200**

```json
{ "id": 2, "username": "zhangsan", "displayName": "张三", "role": "USER" }
```

---

## 9. 个人标签接口（/api/personal/tags）

> 以下接口均须携带 `X-User-Id` 头；数据仅对当前用户可见，与他人的标签互不影响。

### 9.1 列举我的个人标签

`GET /api/personal/tags`

**成功 200**

```json
[
  { "id": 1, "name": "治愈" },
  { "id": 2, "name": "励志" }
]
```

### 9.2 新建个人标签

`POST /api/personal/tags`

**请求体**

```json
{ "name": "治愈" }
```

| 字段 | 约束 |
|------|------|
| name | 必填，≤64 字，同一用户下唯一 |

**成功 201**

```json
{ "id": 1, "name": "治愈" }
```

> 重名时直接返回已有标签（幂等），不会报错。

### 9.3 删除个人标签

`DELETE /api/personal/tags/{tagId}`

删除标签的同时，会清理该标签在所有名言上的关联，避免孤儿数据。

**成功 204**：无响应体。

**失败 404**：`{ "error": "NOT_FOUND", "message": "未找到属于当前用户的标签 id=99" }`

---

## 10. 个人收藏接口（喜好收录，/api/personal）

### 10.1 列举我的收藏

`GET /api/personal/favorites?page=0&size=12`

分页结构同第 1 节；每条名言均携带 `myTags` 与 `favorited` 字段（见第 13 节）。

**成功 200**

```json
{
  "content": [
    {
      "id": 1, "content": "读书患不多，思义患不明。", "author": "韩愈",
      "source": "劝学诗", "popularity": 96, "featured": true, "origin": "SEED",
      "myTags": ["治愈"], "favorited": true
    }
  ],
  "page": 1, "size": 12, "totalElements": 1, "totalPages": 1, "first": true, "last": true
}
```

### 10.2 收藏某条名言

`POST /api/personal/quotes/{quoteId}/favorite`

**成功 201**：无响应体（幂等，重复收藏不报错）。

**失败 404**：名言不存在。

### 10.3 取消收藏

`DELETE /api/personal/quotes/{quoteId}/favorite`

**成功 204**：无响应体。

---

## 11. 名言个人标签接口（归类收录，/api/personal/quotes/{quoteId}/tags）

### 11.1 列举某条名言上的我的个人标签

`GET /api/personal/quotes/{quoteId}/tags`

**成功 200**

```json
[ { "id": 1, "name": "治愈" } ]
```

### 11.2 给某条名言打上个人标签

`POST /api/personal/quotes/{quoteId}/tags`

**请求体**

```json
{ "tagId": 1 }
```

| 字段 | 约束 |
|------|------|
| tagId | 必填，必须是 **属于当前用户** 的标签 id |

**成功 201**：无响应体（幂等，重复打标不报错）。

**失败 404**：`{ "error": "NOT_FOUND", "message": "未找到属于当前用户的标签 id=99" }`（标签不存在或不属于当前用户 / 名言不存在）。

### 11.3 移除某条名言上的某个个人标签

`DELETE /api/personal/quotes/{quoteId}/tags/{tagId}`

**成功 204**：无响应体。

---

## 12. 按个人标签浏览收录

`GET /api/personal/quotes?tagId={tagId}&page=0&size=12`

返回当前用户在该标签下收录的名言（分页同上），每条携带 `myTags`/`favorited`。

**成功 200**

```json
{
  "content": [
    { "id": 1, "content": "读书患不多，思义患不明。", "author": "韩愈", "source": "劝学诗",
      "popularity": 96, "featured": true, "origin": "SEED", "myTags": ["治愈"], "favorited": true }
  ],
  "page": 1, "size": 12, "totalElements": 1, "totalPages": 1, "first": true, "last": true
}
```

**失败 404**：`tagId` 对应的标签不存在或不属于当前用户。

---

## 13. 查询接口中的个人化字段（新增）

当请求携带有效的 `X-User-Id` 头时，`GET /api/quotes`、`GET /api/quotes/{id}`、`GET /api/personal/favorites`、`GET /api/personal/quotes` 等返回的名言对象会额外包含：

| 字段 | 类型 | 说明 |
|------|------|------|
| myTags | String[] | 当前用户打在该名言上的 **个人标签名** 列表 |
| favorited | boolean | 当前用户是否已 **收藏** 该名言 |

不携带 `X-User-Id`（匿名访问）时，这两个字段 **不返回**（保持与旧版前端兼容）。

**带身份查询示例（GET /api/quotes/1，X-User-Id: 2）**

```json
{
  "id": 1,
  "content": "读书患不多，思义患不明。",
  "author": "韩愈",
  "source": "劝学诗",
  "popularity": 96,
  "featured": true,
  "origin": "SEED",
  "createdAt": "2026-10-03T10:20:27.861564",
  "updatedAt": "2026-10-03T10:20:27.861564",
  "myTags": ["治愈"],
  "favorited": true
}
```

> 后端通过一次批量查询（按 `user_id` 聚合标签名映射、名言-标签关联、收藏集合）注入上述字段，避免 N+1 查询，保障列表性能。

---

## 跨域

开发环境下前端（`:5173`）通过 Vite 代理访问，生产部署可配置 `app.cors.allowed-origins` 放开指定域名。
