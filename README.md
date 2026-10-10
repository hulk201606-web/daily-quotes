# 言序 · 每日名言（Daily Quotes）

基于 **Java + Spring Boot** 的后端和 **Vue 3 + Vite** 的前端，实现“每日获取互联网 10 条名人名言”，
并按**知名度 / 出处**展示，同时提供页面化的内容维护能力。

## 环境要求

| 组件 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 17+ | 后端运行必需 |
| Maven | 3.6+ | 构建后端 |
| Node.js | 18+ | 运行前端 |
| npm | 9+（随 Node 自带） | 前端依赖管理 |
| 数据库 | 可选 | H2（内置，零安装）/ MySQL 8 / PostgreSQL |

> 仅体验后端接口时，使用默认的 `h2` profile 即可，无需安装任何数据库。

## 快速开始

```bash
# 1. 启动后端（默认 H2，端口 8080）
java -Dfile.encoding=UTF-8 -jar backend/target/daily-quotes-backend-1.0.0.jar

# 2. 另开一个终端，启动前端（端口 5173）
cd frontend
npm install
npm run dev

# 3. 浏览器访问 http://localhost:5173
```

完整的分库切换、生产构建与自定义采集源说明见下文「运行方式」。

## 功能特性

- **每日自动采集**：每天 06:30（Asia/Shanghai）自动从互联网公开名言库拉取 10 条不重复名言。
- **手动同步**：页面一键“立即采集”，补齐当日 10 条配额。
- **多维度展示**：首页焦点区 + 典藏卡片网格，卡片展示排名、知名度、作者、出处与来源标签。
- **灵活筛选**：支持按关键词搜索、按出处筛选，并按知名度 / 出处 / 作者 / 收录时间排序（升/降序）。
- **内容维护**：通过弹窗新增、编辑、删除名言，表单含实时校验、重复检测与精选标记。
- **多数据库**：ORM 使用 **MyBatis-Plus**，底层支持 **MySQL / PostgreSQL / H2**，profile 一键切换。

## 目录结构

```
daily-quotes/
├─ docs/                   # 设计文档（详细设计 / 数据库设计 / API）
├─ backend/                # Spring Boot 后端（MyBatis-Plus）
│  ├─ src/main/java/...     # 实体、Mapper、服务、控制器、定时任务
│  ├─ src/main/resources/
│  │  ├─ application.yml    # 多数据源 profile（h2 / mysql / pgsql）
│  │  ├─ schema-*.sql       # 各库建表脚本
│  │  └─ logback-spring.xml # 控制台 + 文件日志
│  └─ pom.xml
└─ frontend/               # Vue 3 前端
   ├─ src/components/       # QuoteCard、QuoteModal
   ├─ src/api/quotes.js      # API 封装
   ├─ src/App.vue           # 页面与交互
   └─ vite.config.js
```

## 运行方式

### 1. 后端（端口 8080）

后端内置 H2 / MySQL / PostgreSQL 三种数据源，通过 `--spring.profiles.active` 切换：

| Profile | 数据库 | 缺省连接 | 建表策略 |
|---------|--------|----------|----------|
| `h2`    | H2 文件库（默认，开箱即用） | `./data/daily-quotes` | 启动时执行 `schema-h2.sql` |
| `mysql` | MySQL 8 | `localhost:3306/daily_quotes` | `validate`（需先执行 `schema-mysql.sql`） |
| `pgsql` | PostgreSQL | `localhost:5432/daily_quotes` | `validate`（需先执行 `schema-postgres.sql`） |

```bash
# 默认 H2，无需安装数据库
java -Dfile.encoding=UTF-8 -jar backend/target/daily-quotes-backend-1.0.0.jar

# 切换到 MySQL（先执行 schema-mysql.sql，并在 application.yml 改好账号密码）
java -Dfile.encoding=UTF-8 -jar backend/target/daily-quotes-backend-1.0.0.jar --spring.profiles.active=mysql

# 切换到 PostgreSQL（先执行 schema-postgres.sql，并在 application.yml 改好账号密码）
java -Dfile.encoding=UTF-8 -jar backend/target/daily-quotes-backend-1.0.0.jar --spring.profiles.active=pgsql
```

> 说明：在 GBK 语言环境的 Windows 上，请务必以 `-Dfile.encoding=UTF-8` 启动，否则中文入库会出现乱码。
> `mysql` / `pgsql` 的 `ddl-auto` 默认为 `validate`（保护生产库），建表请使用 `schema-*.sql`。

### 2. 前端（端口 5173）

```bash
cd frontend
npm install
npm run dev
```

浏览器访问 http://localhost:5173 即可。开发环境下前端通过 Vite 代理把 `/api` 转发到后端 8080 端口，无需额外 CORS 配置。

### 3. 生产构建

```bash
cd frontend
npm run build      # 产物位于 frontend/dist
```

## 主要接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET  | `/api/quotes` | 分页/搜索/排序查询，支持 `keyword`、`source`、`sortBy`、`direction`、`page`、`size` |
| GET  | `/api/quotes/stats` | 总量、今日采集量、出处统计 |
| POST | `/api/quotes` | 新增名言（人工维护） |
| PUT  | `/api/quotes/{id}` | 编辑名言 |
| DELETE | `/api/quotes/{id}` | 删除名言 |
| POST | `/api/quotes/fetch` | 手动触发每日互联网采集 |

## 自定义采集来源

修改 `backend/src/main/resources/application.yml`：

```yaml
app:
  quote-fetch:
    url: https://dummyjson.com/quotes/random/{limit}   # {limit} 会被替换为数量
    daily-limit: 10                                    # 每日最多采集条数
    cron: "0 30 6 * * *"                              # 每日执行时间
```

客户端兼容返回结构中的 `quotes`/`quote`/`content`/`author` 字段，方便切换到其它公开名言接口。

## 数据库操作日志

所有数据库读写（分页查询、统计、新增、更新、删除、每日采集写入）都会通过 SLF4J 输出：

- **控制台**：实时打印，含 MyBatis-Plus 执行的 SQL（已格式化）。
- **日志文件**：`backend/logs/daily-quotes.log`，按天滚动，保留 30 天，编码 UTF-8。

业务日志前缀示例：`[DB查询]`、`[DB新增]`、`[DB更新]`、`[DB删除]`、`[DB统计]`、`[DB采集]`、`[DB初始化]`，
便于快速定位数据变更；异常采集会同时记录 WARN/ERROR 日志。

## 技术栈

- 后端：Java 17、Spring Boot 2.7、MyBatis-Plus 3.5、H2 / MySQL 8 / PostgreSQL、RestTemplate、Scheduled、Logback
- 前端：Vue 3、Vite、lucide-vue-next 图标、原生 CSS（编辑风视觉系统，响应式 + 无障碍）

## 设计文档

- `docs/详细设计文档.md`：架构、多库适配、日志与关键设计。
- `docs/数据库设计文档.md`：表结构、字段与跨库兼容性。
- `docs/API文档.md`：全部接口的请求/响应示例。

## 贡献

欢迎提交 Issue 与 Pull Request。开发规范：后端遵循 Java 标准编码风格，前端遵循 Vue 3 `<script setup>` 组合式写法；提交信息建议使用 Conventional Commits 风格（`feat` / `fix` / `docs` / `refactor` 等）。

## 许可证

本项目暂未指定开源许可证。如需对外开源，请先与项目负责人确认后再补充 LICENSE 文件。
