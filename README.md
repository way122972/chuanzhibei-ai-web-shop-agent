# 智能电商导购平台（Smart E-commerce Shopping Guide Platform）

前后端分离项目：**Vue3 + Vue Router + Element Plus**（前端） + **Spring Boot + MyBatis-Plus + MySQL**（后端）。

## 项目结构

```
chuanzhibei-ai-web-shop-agent/      # GitHub 仓库名（本地目录：传智杯项目）
├── backend/                        # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/shopguide/
│       │   ├── ShopGuideApplication.java   # 启动类
│       │   ├── common/Result.java          # 统一返回结果
│       │   ├── common/PageBean.java        # 通用分页封装类（PageBean.of(page) 可在任何分页场景复用）
│       │   ├── config/WebConfig.java       # 跨域 + JWT拦截
│       │   ├── config/MybatisPlusConfig.java # 分页插件（PaginationInnerInterceptor）
│       │   ├── config/StorageProperties.java # 存储配置属性类（@ConfigurationProperties 读取 storage.*）
│       │   ├── config/StorageConfig.java     # 存储配置类（@Bean 有参构造创建 AliyunOSSUtil）
│       │   ├── controller/LoginController.java  # 登录 / 当前用户信息接口
│       │   ├── controller/ProductController.java # 商品接口（分页/新增/上传/上下架/删除）
│       │   ├── controller/CartController.java    # 购物车接口（列表/加购/改数量/删除/清空/结算）
│       │   ├── controller/CategoryController.java # 商品分类接口（全部分类/分类详情）
│       │   ├── entity/User.java / Product.java / Cart.java / Category.java
│       │   ├── interceptor/JwtInterceptor.java   # JWT登录拦截器（写入 ThreadLocal 用户上下文）
│       │   ├── mapper/UserMapper.java / ProductMapper.java / CartMapper.java / CategoryMapper.java
│       │   │    # ProductMapper.selectPageWithCategory = product LEFT JOIN category 多表分页查询
│       │   ├── service/UserService.java / ProductService.java / CartService.java / CategoryService.java / StatisticService.java   # Service 接口
│       │   ├── service/impl/UserServiceImpl.java / ProductServiceImpl.java / CartServiceImpl.java / CategoryServiceImpl.java / StatisticServiceImpl.java  # 实现类
│       │   ├── ai/AiAssistant.java / AiChatService.java / ShopTools.java    # AI 助手（LangChain4j AiServices + Function Calling 工具集）
│       │   ├── ai/http/Utf8HttpClient.java / Utf8HttpClientBuilder.java      # UTF-8 安全的 SSE HTTP 客户端（替代默认 JDK 客户端）
│       │   ├── ws/AiWebSocketHandler.java / WsTokenHandshakeInterceptor.java # AI WebSocket 通道（流式推送 / 握手鉴权）
│       │   ├── vo/TopProductVO.java         # 销售统计 VO
│       │   └── utils/JwtUtils.java / AliyunOSSUtil.java / UserContext.java
│       │       # JwtUtils = 生成/解析 JWT（载荷含 userId）；UserContext = ThreadLocal 用户上下文
│       └── resources/application.yml       # 配置文件（MySQL/JWT/OSS；AI 依赖环境变量 DEEPSEEK_API_KEY）
├── frontend/                       # Vue3 前端
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── api/request.js          # axios封装（自动携带token、401跳登录）
│       ├── api/index.js            # 接口定义
│       ├── router/index.js         # 路由 + 登录守卫
│       ├── store/cart.js           # 购物车状态（对接后端 cart 表）
│       ├── store/category.js       # 商品分类状态（导航子菜单 + 表单下拉框共用）
│       ├── views/Login.vue         # 登录页
│       ├── views/Layout.vue        # 主布局（顶部项目名+用户名/退出登录，左侧导航栏含「商品分类」子菜单）
│       ├── views/ProductList.vue   # 热销商品页 / 商品分类查询页（同一组件，路由 /product 与 /category/:id）
│       ├── views/Cart.vue          # 购物车页（结算/扣款）
│       ├── views/OrderList.vue     # 订单状态页（未支付/已支付切换查询、删除、再来一单）
│       ├── views/Statistic.vue     # 销售数据统计页（ECharts 饼图 + 柱状图，当天/近7天 TOP5）
│       └── components/AiAssistant.vue  # AI 助手右侧对话面板（WebSocket 流式输出）
└── sql/
    └── shop_guide.sql              # 数据库完整脚本（mysqldump 导出：建库 + 4 张表 + 全量数据 + 默认账号 admin/123456）
                                    # 导入方式：mysql -uroot -p < sql/shop_guide.sql
```

## 功能说明

1. **登录**：输入账号密码 → 调用后端 `/user/login` → 数据库校验 → 返回 JWT 令牌（前端存 localStorage）。
2. **主布局**：顶部为项目名称，右上角显示登录用户名，点击“退出登录”清除令牌并返回登录页；左侧为导航栏。
3. **热销商品**：商品模块化卡片（上方图片、下方名称+价格），一行 4 个、一页 8 个（两行），带分页查询条；卡片右下角圆形加号一键加入购物车。
4. **新增 / 修改商品**：页面顶部「新增商品」按钮，弹窗内含商品名称、价格输入框与图片上传框，图片统一上传到
   **阿里云 OSS**（未配置 OSS 时上传接口直接返回错误提示，不写本地磁盘）；点击商品卡片 →「修改商品」会弹出
   同样的对话框并**回显原名称、价格、图片**，「确认修改」后按 id 更新数据库（商品状态不变）。
5. **商品操作**：点击商品卡片弹出操作框 →「下架商品」改变状态（可再次上架）、「修改商品」回显并按 id 更新、「删除商品」从表中删除。
6. **搜索**：商品名称模糊查询 + 价格范围（最低价—最高价）查询 + 分类筛选。
7. **商品分类**：左侧导航「商品分类」下有**生活用品 / 电子产品**两个子菜单（数据来自 `category` 表）。
   分类查询页复用热销商品的卡片布局（一行 4 个、一页 8 个、分页条），通过
   `product LEFT JOIN category` **多表关联查询**该分类下的商品，卡片右上角显示所属分类名称；
   新增 / 修改商品弹窗中可通过下拉框选择商品分类（`product.category_id` 关联 `category.id`）。
   分类页只做浏览展示，不提供「新增商品」入口。
8. **购物车（后端持久化）**：左侧导航「购物车」进入，数据存 `cart` 表；后端通过 JWT 解析当前登录用户并写入 **ThreadLocal（UserContext）**，保证每个用户只能看到自己的购物车。
9. **订单状态**：左侧导航「订单状态」进入，页面**左上角下拉选择 未支付 / 已支付**，
   按当前登录用户查询 `cart` 表对应状态的记录（每条订单一行，展示商品/单价/数量/小计/状态/下单时间）：
   - **未支付**订单可「删除」，删除后购物车中的同一商品同步消失（未支付订单记录即购物车记录）；
   - **已支付**订单显示「再来一单」，点击后按原商品与数量重新加入购物车（角标实时更新）。
10. **结算扣款**：点击「提交购物车（结算）」→ 前端传购物车总价 → 后端按当前用户查询未支付购物车、核对金额、比较 `user.money` 余额：
   - 余额足够 → 扣减余额，购物车记录状态由 **0未支付 → 1已支付**（数据保留，可在「订单状态-已支付」中查看），返回支付金额与剩余余额；
   - 余额不足 → 返回“余额不足！当前余额 ¥x，需支付 ¥y”，不扣款、购物车保持未支付。
11. **无状态鉴权（JWT + ThreadLocal）**：服务端**不保存任何会话文件/会话缓存**。流程为：
   登录成功 → JWT 载荷写入 `userId`、`username` 返回前端 → 前端每次请求头带 `token`
   → `JwtInterceptor` 解析 JWT 取出 `userId`，写入 **ThreadLocal（UserContext）**
   → Service 层用 `UserContext.getRequiredUserId()` 查/存购物车（`cart.user_id`）
   → 请求结束 `afterCompletion` 清理 ThreadLocal，避免线程复用串号。
   退出登录只清除浏览器本地令牌，服务端无需记录状态。
12. **销售数据统计（ECharts）**：左侧导航「销售数据统计」进入，顶部可切换 **当天 / 近7天**，
   后端按 **支付时间**（`cart.update_time`）统计该区间内「已支付」商品的销量，聚合后取前 5 名；
   前端用 **ECharts** 渲染 **饼图**（销售数量占比）与 **柱状图**（销量排行 TOP5，带销售额 tooltip），随窗口自适应。
13. **AI 助手（LangChain4j + DeepSeek + WebSocket）**：顶部用户名左侧有圆形「AI」按钮，
   点击从右侧滑出对话面板，通过 **WebSocket**（`/ws/ai?token=JWT`）与后端交互，回答**逐字流式输出**。
   - 大模型：DeepSeek（`deepseek-chat`，OpenAI 兼容协议），API Key 读取环境变量 **`DEEPSEEK_API_KEY`**，未配置时前端给出提示；
   - Function Calling：模型可自动调用业务工具——按价格/分类查商品、新增/修改/删除商品、加入购物车、查看/清空购物车、查订单，
     例如“推荐 1-100 元的生活用品”会先查库再回答，“帮我清空购物车”会直接执行清空；
   - 会话按 WebSocket 连接隔离（对话记忆 20 条），购物车操作绑定当前登录用户；
   - 工具改动数据后前端自动刷新（购物车角标、商品/订单/统计列表），支持“清空对话”重置记忆；
   - 兼容性处理：自定义 `Utf8HttpClient` 替代 LangChain4j 默认 JDK 客户端，避免中文 Windows（GBK）下 SSE 中文乱码。

## 运行步骤

### 1. 数据库
直接导入 `sql/shop_guide.sql`（MySQL 8）：

```bash
mysql -uroot -p < sql/shop_guide.sql
```

该脚本为 mysqldump 完整导出，自带 `CREATE DATABASE shop_guide`，会一次性创建 `shop_guide` 库、
用户表（含 `money` 余额字段）、商品分类表、商品表（含 `category_id`）、购物车表，并写入全量数据。
默认账号：**admin / 123456**，初始余额 **10000.00**。

### 2. 后端（端口 8081）
修改 `backend/src/main/resources/application.yml` 中的 MySQL 密码：

```yaml
spring:
  datasource:
    username: root
    password: 你的密码
```

图片上传的阿里云 OSS 配置写在 `application.yml` 的 `storage.oss` 下，Endpoint 与 Bucket 直接写，**AK/SK 只放环境变量名，不落明文**：

```yaml
storage:
  oss:
    endpoint: oss-cn-beijing.aliyuncs.com
    access-key-id: ${OSS_ACCESS_KEY_ID:}
    access-key-secret: ${OSS_ACCESS_KEY_SECRET:}
    bucket-name: projectww
```

这组配置由 **`config/StorageProperties`（`@ConfigurationProperties(prefix = "storage")`）** 读取成对象，
再由 **`config/StorageConfig`** 中的 `@Bean` 方法通过**有参构造函数** `new AliyunOSSUtil(storageProperties)` 注入给工具类——
工具类里不再出现 `@Value`，也不是 `@Component`，统一由配置类创建。

`AliyunOSSUtil` 在构造函数里解析密钥，支持三种写法（都能正确工作）：

1. yml 写占位符 `${OSS_ACCESS_KEY_ID:}` → 取同名环境变量；
2. yml 直接写**环境变量名** `access-key-id: OSS_ACCESS_KEY_ID` → 按该名字去读环境变量（读不到则视为未配置，不会把变量名当密钥）；
3. yml 直接写**真实密钥** → 原样使用。

| 环境变量 | 说明 |
|----------|------|
| `OSS_ACCESS_KEY_ID` | 阿里云 AccessKeyId |
| `OSS_ACCESS_KEY_SECRET` | 阿里云 AccessKeySecret |

设置方式任选其一：

```bash
# Windows（永久生效，需重开终端 / 重启 IDEA）
setx OSS_ACCESS_KEY_ID "你的AK"
setx OSS_ACCESS_KEY_SECRET "你的SK"

# macOS / Linux
export OSS_ACCESS_KEY_ID=你的AK
export OSS_ACCESS_KEY_SECRET=你的SK

# 临时（单次启动）
java -DOSS_ACCESS_KEY_ID=你的AK -DOSS_ACCESS_KEY_SECRET=你的SK -jar app.jar
```

IDEA 里运行的话，也可以在 `Run/Debug Configurations → Environment variables` 中填写（更容易临时切换）。

**规则**：图片只能上传到阿里云 OSS，返回 `https://<bucket>.<endpoint>/product/时间戳_uuid.png`；
环境变量未设置（AK/SK 为空）→ 上传接口直接返回「未配置阿里云 OSS，无法上传图片。请设置环境变量 OSS_ACCESS_KEY_ID / OSS_ACCESS_KEY_SECRET」；
OSS 上传报错（AK 错误、bucket 不存在、跨地域等）→ 返回「上传到 OSS 失败：错误码 - 错误信息」。
项目**不提供本地磁盘存储兜底**，`/files/**` 静态映射也已移除。

启动：

```bash
cd backend
mvn spring-boot:run
```

### 3. 前端（端口 5173）

```bash
cd frontend
npm install
npm run dev
```

浏览器打开 http://localhost:5173 ，用 admin / 123456 登录。

## 主要接口一览

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /user/login | 登录，返回 JWT 令牌（载荷含 userId，响应含余额） |
| GET  | /user/info | 当前登录用户信息（用户名、余额），用户身份由 JWT 解析 |
| GET  | /product/page | 分页查询（categoryId 过滤 + name模糊 + 价格范围；product LEFT JOIN category 返回 categoryName） |
| POST | /product/add | 新增商品（含 categoryId） |
| PUT  | /product/update | 修改商品（按 id 更新名称/价格/图片/分类，状态不变） |
| POST | /product/upload | 图片上传（OSS） |
| PUT  | /product/status/{id} | 下架/上架（状态取反） |
| DELETE | /product/delete/{id} | 删除商品 |
| GET  | /category/list | 全部分类（左侧导航「商品分类」子菜单 + 表单下拉框） |
| GET  | /category/{id} | 分类详情 |
| GET  | /cart/list?status=0\|1 | 按状态查询当前登录用户的订单：0=未支付（购物车内容，默认）、1=已支付 |
| GET  | /statistic/top-sales?days=1\|7&limit=5 | 销售数量排行：按支付时间统计区间内「已支付」商品聚合（quantity 求和），days=1 当天、7 近7天，默认前 5 名 |
| WS   | /ws/ai?token=JWT | AI 助手通道：握手时校验 token；客户端发 `{"message":"..."}`，服务端流式回 `ready/start/tool/delta/done/error` |
| POST | /cart/add | 加入购物车（同商品累加） |
| PUT  | /cart/quantity | 修改购物车数量 |
| DELETE | /cart/delete/{id} | 删除购物车商品 |
| DELETE | /cart/clear | 清空购物车 |
| POST | /cart/checkout | 结算：校验余额 → 扣款 → 状态改为已支付 |

> 除 `/user/login` 外，其余接口均需在请求头携带 `token: <JWT令牌>`。
> 当前登录用户由 `JwtInterceptor` 解析 token 后写入 `UserContext`（ThreadLocal），
> Service 层通过 `UserContext.getUserId()` 获取，保证购物车数据严格属于当前用户；请求结束后自动 `clear()` 防止线程复用串号。
