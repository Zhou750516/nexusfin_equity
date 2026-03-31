# 2026-04-01 工作 Checklist

## P0：推动齐为 / 通联补齐真实联调资料

- [ ] 复核 [20260331_齐为通联直连外部确认清单.md](/Users/lixiaokun/Projects/nexusFin/nexusfin-equity/docs/plan/20260331_齐为通联直连外部确认清单.md)
- [ ] 按最小闭环重新确认明天必须拿到的资料：
  - [ ] `syncMemberOrder` 对应接口 / `serviceCode`
  - [ ] `getExerciseUrl` 对应接口 / `serviceCode`
  - [ ] `notifyLending` 对应接口 / `serviceCode`
  - [ ] 请求报文格式与样例
  - [ ] 签名规则与签名字段位置
  - [ ] 响应验签规则与响应样例
  - [ ] 证书用途与双向 TLS 要求
- [ ] 按责任方拆分催办对象：
  - [ ] 齐为
  - [ ] 通联
  - [ ] 我方运维 / 网关
- [ ] 明确每一项若未确认会阻塞哪一步联调
- [ ] 输出一版当天可直接对外同步的话术或清单

## P0：若资料到位，先回填模板再动代码

- [ ] 打开 [20260331_通联直连联调字段待填模板.md](/Users/lixiaokun/Projects/nexusFin/nexusfin-equity/docs/plan/20260331_通联直连联调字段待填模板.md)
- [ ] 回填三个业务动作的真实接口名 / `serviceCode`
- [ ] 回填请求公共字段、业务字段、签名规则
- [ ] 回填响应签名来源、验签口径、成功 / 失败码
- [ ] 回填三个业务动作的关键响应取值字段
- [ ] 自查模板是否仍有空白项会直接阻塞代码实现

## P0：按固定顺序落真实协议实现

- [ ] 先实现请求侧组件：
  - [ ] `AllinpayDirectRequestFactory`
  - [ ] `AllinpayDirectProtocolSerializer`
  - [ ] `AllinpayDirectRequestPreparer`
  - [ ] `AllinpayDirectTransportMapper`
- [ ] 再实现响应侧组件：
  - [ ] `AllinpayDirectResponseSignatureResolver`
  - [ ] `AllinpayDirectResponseVerificationStage`
  - [ ] `AllinpayDirectResponseParser`
- [ ] 保持 `BenefitOrderServiceImpl.createOrder()` 不改业务编排
- [ ] 保持 `BenefitOrderServiceImpl.getExerciseUrl()` 不改业务编排
- [ ] 保持 `NotificationServiceImpl.handleGrant()` 不改业务编排
- [ ] 每完成一段实现就补对应测试，不跳过验证

## P0：若条件具备，启动首轮真实联调

- [ ] 联调顺序按以下执行：
  - [ ] `syncMemberOrder`
  - [ ] `getExerciseUrl`
  - [ ] `notifyLending`
- [ ] 每个接口保留以下证据：
  - [ ] 请求报文
  - [ ] 响应报文
  - [ ] 验签结果
  - [ ] `traceId + bizOrderNo`
  - [ ] 本地状态变化结果
- [ ] 若联调失败，按接口记录问题，不写笼统结论

## P1：若资料未到位，固定阻塞与时间线

- [ ] 将未到位项按责任方和优先级拆分
- [ ] 明确哪些阻塞影响：
  - [ ] 真实请求出站
  - [ ] 响应验签
  - [ ] 业务解析
  - [ ] 首轮联调启动时间
- [ ] 更新阻塞说明文档或日报口径
- [ ] 给出“能否启动真实联调”的明确判断，不使用模糊描述

## 收尾检查

- [ ] 回看 [20260401.md](/Users/lixiaokun/Projects/nexusFin/nexusfin-equity/docs/plan/20260401.md) 与明日实际执行是否一致
- [ ] 明确明天结束时属于 A 档还是 B 档结果
- [ ] 若拿到新资料，先更新模板和清单，再做代码实现
- [ ] 若未拿到新资料，不再继续做泛化基础设施改造

## 明天结束时的完成标准

1. 要么拿到足够的协议资料，进入真实协议实现或真实联调。
2. 要么把缺失项、责任方、交付影响写清楚，不再反复判断同一阻塞。
