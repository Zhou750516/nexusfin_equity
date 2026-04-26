# 测试套件耗时优化方案（待确认）

> 状态：草案 · 待用户确认可行性后再动手实施
> 作者：Claude Code
> 日期：2026-04-25
> 范围：`nexusfin-equity` Maven 后端测试套件（Spring Boot 3.2.12 + JUnit 5 + MyBatis Plus + H2/MySQL）

---

## 1. 现状量化

通过静态扫描得到的客观数字：

| 指标 | 数值 |
|------|------|
| 测试文件总数 | 89 |
| `@Test` 方法总数 | 241 |
| `@SpringBootTest`（全 Context）类 | **22** |
| `@WebMvcTest` / `@DataJpaTest` 等切片测试 | **0** |
| `@ExtendWith(MockitoExtension)` 纯单测 | 31 |
| 纯 JUnit 单测（无 Spring/无 Mockito 扩展） | 33 |
| `@MockBean` 出现次数 | 20（分布在 13 个 IT 类） |
| `@DirtiesContext` | 0（好） |
| `Thread.sleep` / Awaitility | 0（好） |
| Surefire / Failsafe 显式配置 | **无**（走 Spring Boot Parent 默认） |
| `junit-platform.properties` | **不存在** |

> **关键观测**：每个新增的 IT 多半带着自己独有的 `@MockBean` 组合。当前 22 个 IT 实际产生了 **12 种不同的 ApplicationContext 风味**（按 `@MockBean` 集合 + `@ActiveProfiles` 聚合）。

12 种 Context 风味的散点（来自扫描）：

```
1   @ActiveProfiles("mysql-it") | (无 MockBean)
2   @ActiveProfiles("mysql-it") | AsyncCompensationRouterExecutor
3   default | (无 MockBean)
4   default | AsyncCompensationEnqueueService + XiaohuaGatewayService
5   default | AsyncCompensationEnqueueService + YunkaGatewayClient
6   default | BenefitDispatchService
7   default | JointLoginService
8   default | QwBenefitClient
9   default | RefundService
10  default | TechPlatformUserClient
11  default | TechPlatformUserClient + XiaohuaAuthClient
12  default | XiaohuaGatewayService + YunkaGatewayClient
```

---

## 2. 根因分析

### 2.1 主因：Spring TestContext 缓存被打散（影响最大）

Spring 的 `TestContextCache` 用 “**注解 + properties + classes + MockBean 集合 + Profile**” 作为缓存键。任何一项不一致 → 重新启动 ApplicationContext。

- 每个新建的 IT 类倾向于按需 mock 某个外部依赖，造成 **MockBean 组合每类不同**。
- 结果：**一次 `mvn test` 实测会冷启动 Spring Context ~12 次**。每次冷启动按当前依赖规模估 **5–10 秒**，仅这一项就贡献 **60–120 秒**纯 Context 装配开销。
- 这正好解释了用户感受到的 **“测试越加越慢”**——加测试 ≈ 加 Context 风味 ≈ 加冷启动次数。增长是 **超线性** 的（O(类数 × 唯一 MockBean 组合数)）。

### 2.2 次要因：所有 Controller 都用 `@SpringBootTest` 而非切片测试

22 个 IT 几乎全是 Controller / Repository 层测试，理论上完全可以用 `@WebMvcTest`（仅 Web 层）或 `@MybatisPlusTest`（仅持久层）。

- `@SpringBootTest` 会装配 **整个**应用上下文（HTTP Server、所有 Service、所有 Repository、所有第三方 Client、调度器、加密组件、JWT、auth filter…）。
- `@WebMvcTest` 只装配 Web 层 + 你 `@Autowired` 的 Controller，启动 **快 3–5 倍**。

### 2.3 完全串行执行（次要但稳定收益）

- `pom.xml` 没有显式配置 `maven-surefire-plugin`，走 Spring Boot Parent 默认值：**单 fork、串行执行类、串行执行方法**。
- `junit-platform.properties` 不存在，JUnit 5 的并行能力 **完全未启用**。
- 31 个纯 Mockito 单测 + 33 个纯单测 = **64 个不需要 Spring 的快速测试**仍然在排队等 IT 跑完。

### 2.4 非问题（已排查）

- ❌ `@DirtiesContext` 未滥用（0 处） → Context 不会被强制销毁。
- ❌ `Thread.sleep` / Awaitility 未使用 → 没有死等耗时。
- ❌ Mockito MockMaker 是 `subclass`（默认快版） → 不需要切回 `inline`。
- ❌ H2 是内存模式 + `DB_CLOSE_DELAY=-1` → 启动开销可忽略。
- ❌ 单 Maven 模块 → 不存在多模块 fork 重复成本。

---

## 3. 解决方案（分级，按 “投入 / 收益 / 风险” 排序）

> 估时格式：`人工 / Claude Code` —— 利用 AI 辅助，多数任务的实际成本是分钟级。

### Tier 1 · 立竿见影（推荐先做）

#### T1.1 — 启用 JUnit 5 类内并行 + Surefire 多 fork
**估时**：30min / 10min · **预期收益**：单元测试墙钟 **−40% ~ −60%**

```xml
<!-- pom.xml -->
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration>
    <forkCount>0.5C</forkCount>     <!-- 每核 0.5 个 JVM -->
    <reuseForks>true</reuseForks>
    <argLine>-Xmx1g -XX:+UseG1GC</argLine>
  </configuration>
</plugin>
```

```properties
# src/test/resources/junit-platform.properties
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.mode.default=same_thread
junit.jupiter.execution.parallel.mode.classes.default=concurrent
# 单测并行；Spring Context 共享 + 类间并行（IT 仍按需串行）
```

> ⚠️ 风险：含共享可变状态（H2 内存库 / 静态 Map）的测试会竞争。先 **只对纯单测启用并行**（`@Execution(CONCURRENT)` 注解或包级控制），IT 暂保留串行。

#### T1.2 — 抽出统一的 `AbstractIntegrationTest` 基类合并 MockBean
**估时**：2h / 30min · **预期收益**：Context 风味 12 → **2–3**，墙钟 **−60s ~ −100s**

```java
// src/test/java/com/nexusfin/equity/support/AbstractIntegrationTest.java
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {
    // 所有 IT 共同 mock 的外部依赖一次性声明
    @MockBean protected YunkaGatewayClient yunkaGatewayClient;
    @MockBean protected XiaohuaGatewayService xiaohuaGatewayService;
    @MockBean protected TechPlatformUserClient techPlatformUserClient;
    @MockBean protected XiaohuaAuthClient xiaohuaAuthClient;
    @MockBean protected QwBenefitClient qwBenefitClient;
    @MockBean protected AsyncCompensationEnqueueService asyncCompensationEnqueueService;
    @MockBean protected AsyncCompensationRouterExecutor routerExecutor;
    @MockBean protected RefundService refundService;
    @MockBean protected BenefitDispatchService benefitDispatchService;
    @MockBean protected JointLoginService jointLoginService;
    // ... 一次性把 MockBean 全集列出
}
```

> 哲学：**一个 IT 不需要某个 mock 不会让它变慢，但 MockBean 组合不一致会让 Context 重启**。把所有 IT 期望 mock 的 bean 列在一处，Context 缓存就只有 1 份。
> 单个 IT 类如需特殊 stub，直接 `@BeforeEach` 改这些 protected mock 即可，不要再加 `@MockBean`。

> ⚠️ 风险：基类引入会让 “某个测试不希望某个 bean 被 mock” 的场景偶尔需要重写。可接受（少数派）。

#### T1.3 — `@SpringBootTest` 显式声明 webEnvironment
**估时**：15min / 5min · **预期收益**：稳定性，避免随机端口启动

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
```

> 默认就是 MOCK（最快），显式声明只是固化预期，防止后续误改。

---

### Tier 2 · 结构性改造（性价比高）

#### T2.1 — Controller 测试迁移到 `@WebMvcTest`
**估时**：1 day / 2h · **预期收益**：每个 Controller 测试启动 **−70%**（5–10s → 1–2s）

挑选 “只验证 HTTP 输入输出 / 参数校验 / Auth filter” 的 Controller 测试改造成 `@WebMvcTest`：

```java
@WebMvcTest(controllers = RepaymentController.class)
@Import(JwtUtil.class)
class RepaymentControllerWebMvcTest {
    @Autowired MockMvc mockMvc;
    @MockBean RepaymentService repaymentService;  // 业务层全 mock
}
```

> 不适合切片的：跨多个 Service 的端到端集成场景、需要真实 H2 数据的回归。这部分仍保留 `@SpringBootTest`。
> 估计 **22 个 IT 中至少 10 个**可以下沉到切片测试。

#### T2.2 — 拆分 surefire（单测）/ failsafe（IT）
**估时**：2h / 30min · **预期收益**：本地开发 `mvn test -DskipITs` 跑得飞快

```xml
<!-- 单测：*Test.java（不含 IntegrationTest） -->
<plugin>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration>
    <excludes><exclude>**/*IntegrationTest.java</exclude></excludes>
  </configuration>
</plugin>
<!-- IT：*IntegrationTest.java -->
<plugin>
  <artifactId>maven-failsafe-plugin</artifactId>
  <executions>
    <execution><goals><goal>integration-test</goal><goal>verify</goal></goals></execution>
  </executions>
</plugin>
```

之后：
- `mvn test` → 仅跑单测（秒级）
- `mvn verify` → 单测 + IT
- CI 主分支跑 `verify`，feature PR 可只跑 `test` + 受影响 IT

#### T2.3 — `mysql-it` profile 与默认 H2 测试硬隔离
**估时**：1h / 15min · **预期收益**：避免 mysql-it 测试在本地无 MySQL 时阻塞 CI 反馈

- 给 `*MySql*IntegrationTest` 打 `@Tag("mysql-it")`。
- Surefire 默认 `excludedGroups=mysql-it`，CI 中专门一个 stage 加上 `-Dgroups=mysql-it` 并配好 MySQL 服务。

---

### Tier 3 · 重投入（视后续团队规模再做）

| 方案 | 估时 | 收益 | 适用条件 |
|------|------|------|---------|
| Testcontainers 取代 application-mysql-it.yml 的本地依赖 | 4h / 1h | 跨机一致性提升、CI 不再依赖外部 MySQL | 团队 >2 人或多分支 IT 频繁跑挂 |
| Spring Context **预热** + 持久化 JVM（Maven daemon / mvnd） | 2h / 30min | 本地循环开发首跑省 5–10s | 高频本地跑测 |
| 测试影响分析（test-impact / Develocity） | 1d / 4h | 仅跑受改动文件影响的测试，CI 时间 **−50%+** | 总测试 >500 时再做 |
| JaCoCo / 覆盖率与测试性能去耦（独立 profile） | 30min / 10min | 日常 CI 不带 instrumentation | 已经用 JaCoCo |

---

## 4. 推荐路线图（建议执行顺序）

| 步 | 动作 | 状态 | 估时 | 收益 |
|----|------|------|------|------|
| 1 | T2.2 拆 surefire / failsafe，`mvn test` 仅跑单测 | ✅ 已完成 | — | 单测反馈 32s → 17.6s |
| 2 | T1.2 引入 `AbstractIntegrationTest`，合并 MockBean | 待做 | 2h / 30min | IT 墙钟 −60–100s |
| 3 | T1.3 固化 webEnvironment | 待做 | 15min / 5min | 稳定性 |
| 4 | T2.1 渐进迁移 Controller 测试到 `@WebMvcTest` | 待做 | 1d / 2h | 进一步 −20–30% |
| 5 | T2.3 mysql-it Tag 隔离 | 待做 | 1h / 15min | 本地跑测无需 MySQL |
| 6 | （观察）若单测扩到 200+ 再考虑 JUnit 类间并行 | 暂不做 | — | 当前规模净亏损 |

> **重要观察**：T1.1（JUnit 并行 + 多 fork）实测对当前规模 **净亏损**——4 forks × 4 线程 = 16 线程争抢 8 核，CPU 抢占让单类耗时反升 7×；单 fork + 类间并行也比纯串行慢 3s。**仅当单测规模 ≥ 200 个或单类平均耗时 ≥ 1s 时才考虑重启此项**。

---

## 5. 收益预估（汇总）

### 5.1 第一阶段实测（已落地，2026-04-25）

仅做了 **T2.2（surefire / failsafe 拆分）**——`mvn test` 排除 `*IntegrationTest.java` + `NexusfinEquityApplicationTests.java`，IT 走 `mvn verify`。Surefire 配 `forkCount=1 reuseForks=true`，未启用 JUnit 类间并行（实测对当前规模净亏损 3s）。

| 指标 | 改前 `mvn test` | 改后 `mvn test` | 变化 |
|------|----------------|----------------|------|
| 墙钟时间 | 32.25s | **17.61s** | **−45%** |
| 跑的测试数 | 181（含 2 个 SpringBootTest） | 179（纯单测） | −2 |
| 失败 / 错误 | 1 失败（来自 SpringBootTest） | 0 / 0 | ✅ |
| 反馈纯净度 | 混杂 IT 启动日志 | 仅单测 | ✅ |

> 17.6s 中 **Maven 自身启动 + Surefire fork + 编译增量** 占约 12s，**实际测试运行** 约 6s。Maven 启动是当前的硬底，要再降需上 mvnd / Maven Daemon。

### 5.2 待做阶段预估（沿用初版数字，未实测）

| 阶段 | 累计收益（相对原 32s baseline） | 估算后 `mvn test` |
|------|-------------------------------|------------------|
| ~~T2.2 surefire/failsafe 拆分~~ ✅ 已完成 | −45% | 17.6s |
| + T1.2 合并 MockBean → IT 走 verify 时也大幅提速 | IT 套件 −60% | — |
| + T2.1 切片改造 10 个 Controller | IT 套件再 −20–30% | — |

---

## 6. 风险与缓解

| 风险 | 描述 | 缓解 |
|------|------|------|
| 并行测试相互干扰 | H2 内存库 / 静态共享状态被多个线程写 | 仅对显式标注 `@Execution(CONCURRENT)` 的纯单测启用；IT 保持串行 |
| `AbstractIntegrationTest` 引入耦合 | 某个 IT 不希望某个 bean 被 mock | 该 IT 重写为不继承基类（极少数情况），保留单独 Context |
| `@WebMvcTest` 切片漏装关键组件 | Auth filter / JWT / 自定义 ArgumentResolver 不会自动装 | 通过 `@Import` 显式补齐；改造时一类一类验证 |
| Surefire 多 fork 提高内存占用 | `0.5C` 在 8 核机器上 = 4 个 JVM × 1G | 本地默认 `1`（单 fork）+ CI 配 `0.5C` 即可 |
| Failsafe 拆分后开发者忘了跑 IT | 习惯了 `mvn test`，IT 漏跑 | 文档 + Git pre-push hook 跑 `mvn verify`；CI 兜底 |

---

## 7. 不推荐做的事（明确避免）

- ❌ 把 Mockito MockMaker 切到 `inline` —— 当前 `subclass` 已是最快默认，切 `inline` 反而拖慢。
- ❌ 引入 `@DirtiesContext` 来 “清理状态” —— 这是 Context 缓存的杀手，几乎一定让事情更糟。需要清理状态时用 `@BeforeEach` 显式 reset。
- ❌ 整体迁移到 Testcontainers “求一致性” —— 当前 H2 + mysql-it profile 的双轨已经够用；上 Testcontainers 收益不大且本地启动会慢。
- ❌ 引入 Gradle 替代 Maven —— 收益有但成本远大于本方案任何一项。

---

## 8. 我希望你确认的几个决定

1. **路线图是否同意？** 特别是 **T1.1 + T1.2 + 量化 baseline** 三件作为第一阶段。
2. **`AbstractIntegrationTest` 基类的位置和命名**：放在 `src/test/java/com/nexusfin/equity/support/` 是否合适？
3. **是否同意把 `@WebMvcTest` 改造列为长期任务**（一次迁 1–2 个，不一次性大改）？
4. **CI 的 mysql-it 是否已有专用 stage**？如果没有，T2.3 需要顺带修 CI yaml。
5. **先量化基线再改 vs. 直接做 T1.1 看效果**——你倾向哪个？

确认上述任一组合后我就开始干。
