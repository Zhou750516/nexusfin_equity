# NexusFin 跨域认证设计方案

> **版本**: 1.0 | **日期**: 2026-03-24

---

## 1. 业务背景

用户在科技平台授信被拒后，需要导流至艾博生页面购买权益。这个过程涉及两个独立部署的系统（科技平台、艾博生），需要解决两个问题：

- 用户从科技平台跳转到艾博生时，**自动保持登录状态**，无需重新登录。
- 用户直接访问艾博生页面时，**显示未登录状态**。

云卡系统面临同样的问题，方案通用。

## 2. 设计原则

- **科技平台零改动**：复用科技平台已有的登录验证接口，不新增接口。
- **艾博生最小改动**：仅新增一个 SSO 回调接口。
- **取消静默注册**：不再需要科技平台调用艾博生的 `/api/users/register` 接口，用户数据在首次跳转时从科技平台接口即时获取并创建。

## 3. 核心流程

### 3.1. 跨域自动登录流程

整个流程分为三步：**凭证传递 → 身份校验 → 会话创建**。

**第一步：凭证传递。** 用户在科技平台被拒后点击跳转，科技平台前端从本地（Cookie 或 LocalStorage）获取当前用户的登录凭证 `tech_token`，将其作为 URL 参数拼接到艾博生的回调地址上，发起 302 重定向。

> 跳转 URL：`https://equity.nexusfin.com/api/auth/sso-callback?token=<tech_token>`

**第二步：身份校验。** 艾博生后端收到请求后，从 URL 中提取 `tech_token`，然后通过**服务端到服务端（S2S）**的方式调用科技平台已有的登录验证接口（如 `GET /api/users/me`），将 `tech_token` 放在 `Authorization` 请求头中发送。科技平台验证 Token 有效后，返回该用户的基本信息（userId、phone、realName 等）。

**第三步：会话创建。** 艾博生根据返回的用户信息，在本地数据库中查找该用户。如果用户不存在则即时创建（**即时用户同步，JIT Provisioning**），如果已存在则直接使用。随后，艾博生生成自己的本地 JWT，通过 `Set-Cookie` 响应头种入用户浏览器，最后将用户 302 重定向到权益购买首页。

### 3.2. 时序图

![跨域自动登录时序图](https://private-us-east-1.manuscdn.com/sessionFile/lM8FPhDS949gd7J4r0ncb4/sandbox/AQNgGHL2SMVuUCspSsDqCU-images_1774339225909_na1fn_L2hvbWUvdWJ1bnR1L3Nzb192Ml9zZXF1ZW5jZQ.png?Policy=eyJTdGF0ZW1lbnQiOlt7IlJlc291cmNlIjoiaHR0cHM6Ly9wcml2YXRlLXVzLWVhc3QtMS5tYW51c2Nkbi5jb20vc2Vzc2lvbkZpbGUvbE04RlBoRFM5NDlnZDdKNHIwbmNiNC9zYW5kYm94L0FRTmdHSEwyU01WdVVDc3BTc0RxQ1UtaW1hZ2VzXzE3NzQzMzkyMjU5MDlfbmExZm5fTDJodmJXVXZkV0oxYm5SMUwzTnpiMTkyTWw5elpYRjFaVzVqWlEucG5nIiwiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6eyJBV1M6RXBvY2hUaW1lIjoxNzk4NzYxNjAwfX19XX0_&Key-Pair-Id=K2HSFNDJXOU9YS&Signature=P96oUcJrKt6mc4NH1dIy5jXLBXImVo4Nxt~uwACjCYaiiVtPlnR3N3CwCTz7lL5jn0X9vcvJ8VX2f0AcybrZDMb8-vG6sokZySwgh8f6SxJXahI0HZ4jWTn95JAK1ykRCIb8u-AP52uDQ5PkE8Vwj2MlgG7TScZoyIfszzUMOgJyfnDnAM-iO501IEonrRS1FlhZMr4tqO6N~0nmqbqyYRgQElg1z0x6x5EAYdsYTBdWpGHp1r7C99OKZxAttndqO~GPr5okKTDfXlHFXPf7e7VVT8AAeVdV~AssN94aZxNGr8KJr~XDRZGNIXJgBLlrqJirhzf8BNr91Cu~BM4Tow__)

### 3.3. 独立访问的处理

不需要做任何特殊判断。艾博生前端在每个页面加载时，统一检查浏览器中是否存在合法的 JWT Cookie：

- **存在且有效** → 调用 `/api/users/me` 校验通过 → 渲染已登录视图。
- **不存在或无效** → 渲染未登录/游客视图。

从科技平台跳转过来的用户，因为经历了上述 SSO 流程，浏览器中已被种下合法 Cookie，所以显示已登录。直接访问的用户，浏览器中没有该 Cookie，所以显示未登录。**两种场景通过同一套机制自然区分。**

![前端登录状态检查流程](https://private-us-east-1.manuscdn.com/sessionFile/lM8FPhDS949gd7J4r0ncb4/sandbox/AQNgGHL2SMVuUCspSsDqCU-images_1774339225909_na1fn_L2hvbWUvdWJ1bnR1L2Zyb250ZW5kX2F1dGhfZmxvdw.png?Policy=eyJTdGF0ZW1lbnQiOlt7IlJlc291cmNlIjoiaHR0cHM6Ly9wcml2YXRlLXVzLWVhc3QtMS5tYW51c2Nkbi5jb20vc2Vzc2lvbkZpbGUvbE04RlBoRFM5NDlnZDdKNHIwbmNiNC9zYW5kYm94L0FRTmdHSEwyU01WdVVDc3BTc0RxQ1UtaW1hZ2VzXzE3NzQzMzkyMjU5MDlfbmExZm5fTDJodmJXVXZkV0oxYm5SMUwyWnliMjUwWlc1a1gyRjFkR2hmWm14dmR3LnBuZyIsIkNvbmRpdGlvbiI6eyJEYXRlTGVzc1RoYW4iOnsiQVdTOkVwb2NoVGltZSI6MTc5ODc2MTYwMH19fV19&Key-Pair-Id=K2HSFNDJXOU9YS&Signature=SiU5uuheDtWL44L--tAVqAaqnVNq7xM2D3Uq7h~027hEOni-R-TVPDm-f-nd6IH5BjsSqpiP9abkUwuw6eWdCgYDjw8nY-xuf4Li2kW05UnmScqxOWa1iRC9UmP2xU8sEd3YvUrxvZY9ybfRQuLD7vmoXhCVYi9niRrZ2m-o8AlOAFMQd53H3kP6l8pbYvKRqlhFfEftdFE70etAyEuBbGUvFLk2imp79EA2bXdG6EhSSJ~w501-ceR6oy~0UJiaY6-qhXjuo6EUyq3mGC3tQ1Yfy~IlsPJm2zoU-DpOfUO6SoDT5h6R9HoUj9rAmzByqw5ko8evE-4GeFQEPe-8Ow__)

## 4. 接口设计

### 4.1. 艾博生新增接口

**SSO 回调接口**

| 项目 | 说明 |
| :--- | :--- |
| 路径 | `GET /api/auth/sso-callback` |
| 调用方 | 用户浏览器（由科技平台 302 重定向触发） |

请求参数：

| 参数 | 类型 | 必须 | 描述 |
| :--- | :--- | :--- | :--- |
| token | String | 是 | 科技平台的登录凭证 |
| redirect_url | String | 否 | 登录成功后跳转的目标页面，需做白名单校验 |

处理逻辑：

1. 从 URL 中获取 `token`。
2. 调用科技平台 `GET /api/users/me`（Header 中携带 `Authorization: Bearer <token>`）。
3. 校验成功后，根据返回的 `userId` 查找或创建本地用户。
4. 生成艾博生本地 JWT，通过 `Set-Cookie` 种入浏览器。
5. 302 重定向到目标页面。

**用户信息接口**

| 项目 | 说明 |
| :--- | :--- |
| 路径 | `GET /api/users/me` |
| 调用方 | 艾博生前端 |

功能：校验当前 JWT Cookie 的有效性，返回用户信息。供前端判断登录状态使用。

### 4.2. 科技平台复用的接口

| 项目 | 说明 |
| :--- | :--- |
| 路径 | `GET /api/users/me`（或科技平台已有的等效接口） |
| 调用方 | 艾博生后端（S2S） |

功能：接收 `Authorization: Bearer <tech_token>`，验证 Token 有效性，返回用户信息（userId、phone、realName 等）。**此接口为科技平台已有能力，无需新增。**

## 5. 对现有设计的影响

| 影响范围 | 说明 |
| :--- | :--- |
| 科技平台后端 | **无改动** |
| 科技平台前端 | 跳转时在 URL 中拼接已有的 `tech_token`，改动极小 |
| 艾博生后端 | 新增 SSO 回调接口 + `/api/users/me` 接口 |
| 艾博生前端 | 页面加载时增加登录状态检查逻辑；页面加载后用 `history.replaceState()` 清理 URL 中的 token 参数 |
| 静默注册接口 | **取消**，`/api/users/register` 不再需要 |
| 数据库 | 无需新增表，用户表 `t_user` 增加 `tech_platform_user_id` 字段作为关联外键 |
| 已有业务接口 | **无影响**，22 个业务接口通过统一的 JWT 认证过滤器校验登录状态即可 |
| 订单状态机 | **无影响**，认证流程在业务流程之前完成 |

## 6. 安全措施

- **强制 HTTPS**：所有系统间通信必须基于 HTTPS。
- **URL 清理**：艾博生前端在页面加载后，立即使用 `history.replaceState()` 移除 URL 中的 token 参数，防止通过浏览器历史泄漏。
- **Cookie 安全属性**：JWT Cookie 必须设置 `HttpOnly`（防 XSS）、`Secure`（仅 HTTPS）、`SameSite=Lax`（防 CSRF）。
- **重定向白名单**：`redirect_url` 参数必须做域名白名单校验，防止开放重定向漏洞。

## 7. 云卡复用

云卡系统面临完全相同的认证场景（用户从艾博生跳转到云卡进行借款撮合）。云卡只需实现同样的 SSO 回调接口和用户信息接口，复用相同的流程即可。
