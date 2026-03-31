# QW Allinpay Direct Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不改变现有业务入口的前提下，为齐为适配层新增通联证书直连模式的配置、路由和 client 骨架能力。

**Architecture:** 保留 `QwBenefitClient` 作为上层业务接口，新增 `ALLINPAY_DIRECT` 模式、协议路由层、证书装载器和直连 client 骨架。当前只落地配置与骨架，不实现缺少外部协议文档的具体报文组装逻辑。

**Tech Stack:** Java 17, Spring Boot 3.2, JUnit 5, Mockito, AssertJ, Spring `ConfigurationProperties`

---

### Task 1: 扩展配置模型

**Files:**
- Modify: `src/main/java/com/nexusfin/equity/config/QwProperties.java`
- Modify: `src/main/resources/application.yml`
- Modify: `src/test/resources/application.yml`
- Modify: `src/test/resources/application-mysql-it.yml`
- Test: `src/test/java/com/nexusfin/equity/thirdparty/qw/QwPropertiesTest.java`

- [x] 增加 `QwProperties.Mode` 的 `QWEIMOBILE_HTTP` 与 `ALLINPAY_DIRECT`
- [x] 增加 `direct` 配置分组，承载通联直连参数
- [x] 同步主配置和测试配置默认值
- [x] 编写配置单元测试，验证 `direct` 配置对象可读写

### Task 2: 落地协议路由层

**Files:**
- Create: `src/main/java/com/nexusfin/equity/thirdparty/qw/RoutingQwBenefitClient.java`
- Modify: `src/main/java/com/nexusfin/equity/thirdparty/qw/QwBenefitClientImpl.java`
- Test: `src/test/java/com/nexusfin/equity/thirdparty/qw/RoutingQwBenefitClientTest.java`

- [x] 先写测试，验证 `MOCK/QWEIMOBILE_HTTP` 走现有实现，`ALLINPAY_DIRECT` 走新实现
- [x] 将现有 `QwBenefitClientImpl` 视为聚合网关实现
- [x] 新增 `@Primary` 路由层，避免上层业务感知协议差异

### Task 3: 落地证书装载基础设施

**Files:**
- Create: `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayCertificateLoader.java`
- Test: `src/test/java/com/nexusfin/equity/thirdparty/qw/AllinpayCertificateLoaderTest.java`

- [x] 先写测试，验证可从 `PKCS12` 证书包和 `PEM/DER` 证书文件加载材料
- [x] 实现 `user-rsa.p12` 装载
- [x] 实现 `public-rsa.cer` / `allinpay-pds.cer` 的 `X509Certificate` 装载
- [x] 对路径缺失、证书格式错误输出明确异常

### Task 4: 落地通联直连 client 骨架

**Files:**
- Create: `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectQwBenefitClient.java`
- Test: `src/test/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectQwBenefitClientTest.java`

- [x] 先写测试，验证直连 client 会校验关键配置并装载证书
- [x] 实现 skeleton：构造期完成配置校验和证书准备
- [x] 三个业务方法先输出明确的“协议资料未补齐 / skeleton 未完成”异常
- [x] 保证异常文案能明确区分为“配置缺失”还是“协议未落地”

### Task 5: 文档收口与针对性验证

**Files:**
- Modify: `docs/superpowers/specs/2026-03-31-qw-allinpay-direct-design.md`
- Create: `docs/plan/20260331_通联直连实现说明.md`

- [x] 补充已实现的代码落地结果与限制说明
- [x] 记录新增配置项、模式切换方法、当前 skeleton 边界
- [x] 运行新增测试和受影响测试
- [x] 记录验证命令与结果

### Task 6: 外部协议回填前的联调准备

**Files:**
- Create: `docs/plan/20260331_通联直连联调执行清单.md`
- Modify: `docs/plan/20260331_通联直连实现说明.md`
- Modify: `docs/plan/20260331_通联直连联调字段待填模板.md`

- [x] 补一份面向齐为 / 通联对齐的联调执行清单
- [x] 将环境配置映射、代码替换点、外部阻塞项整理到单页文档
- [x] 在实现说明中补充执行清单入口，避免后续重复判断现状

### Task 7: 外部资料到位后的第二阶段实现

**Files:**
- Modify: `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectProtocolSerializer.java`
- Modify: `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectTransportMapper.java`
- Modify: `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectResponseVerificationStage.java`
- Modify: `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectResponseParser.java`
- Modify: `docs/plan/20260331_通联直连联调字段待填模板.md`

- [ ] 根据齐为 / 通联正式协议资料回填三个业务动作的 `serviceCode`
- [ ] 将 skeleton serializer 替换为真实请求协议实现
- [ ] 将 skeleton verification / parser 替换为真实回包验签和解析实现
- [ ] 追加联调样例、失败样例和证据记录
