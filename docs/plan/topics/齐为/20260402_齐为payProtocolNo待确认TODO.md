# 2026-04-02 齐为 `payProtocolNo` 待确认 TODO

## 1. 背景

截至 `2026-04-02`，齐为已明确回复：

1. 会员同步后开卡即成功
2. `openFlag=0` 表示开卡成功
3. `abs.token.get` 只传 `uniqueId + partnerOrderNo` 可以
4. 不需要额外传 `orderNo / cardNo`
5. 当前 `515 会员卡未生效` 的真正原因是：
   - 扣款失败
   - 扣款失败原因是支付协议号错误

因此当前齐为问题已收敛为：

> 需要确认我方传给 `abs.push.order` 的 `payProtocolNo`，到底应该来自哪个真实系统字段，而不是继续排查 `abs.token.get` 参数。

## 2. 下午待确认事项

- [x] 确认当前业务里“真实支付协议号”来自哪个系统 / 哪个字段
- [x] 确认 `BenefitOrder.agreementNo` 是否只是内部协议号，不应直接透传给齐为
- [x] 确认当前回退值 `proto- + benefitOrderNo` 是否仅为占位值，不能用于真实扣款
- [ ] 确认后续如果修复 `payProtocolNo`，最小改造点涉及哪些代码和数据字段

## 3. 下午确认结果

本轮已确认如下：

1. 真实 `payProtocolNo` 来自：
   - 艾博生与第三方支付公司 **通联** 签约后返回的协议号
2. 协议号规则：
   - 一个用户一个协议号
3. 当前项目现状：
   - 目前 **还没有** 和通联完成真实对接
   - 因此当前项目内并没有真实可用的支付协议号来源
4. 当前联调安排：
   - 齐为已提供一个 **mock 协议号**，用于继续联调：
     - `AIP211926033187CF73483`
5. 后续建设要求：
   - 需要在 `docs/third-part` 下面补充 **通联服务对接流程**
   - 目标是后续能真实获取支付协议号并接入主链

## 4. 当前代码位置

当前 `payProtocolNo` 赋值逻辑位于：

- `src/main/java/com/nexusfin/equity/service/impl/BenefitOrderServiceImpl.java:201`

当前逻辑为：

1. `agreementNo` 非空时，直接使用 `agreementNo`
2. `agreementNo` 为空时，回退为 `proto-` + `benefitOrderNo`

## 5. 当前判断

当前高概率情况是：

1. `agreementNo` 是我方内部协议/合同标识
2. 它并不等于齐为可识别的真实支付协议号
3. 因此 `abs.push.order` 虽然建单成功，但扣款失败，最终导致会员卡未生效
4. 当前 `proto-` 前缀回退值也只是联调用占位值，不能当成真实支付协议号长期使用

## 6. 下午确认后的下一步

### 情况 A：找到真实支付协议号来源

则后续执行：

- [x] 先接收齐为提供的 mock 协议号
- [x] 输出“mock 协议号联调方案”和最小改造方案
- [x] 用 mock 协议号重新真实联调 `abs.push.order` + `abs.token.get`
- [x] 回填 bug / 部署 / checklist 文档
- [ ] 后续补 `docs/third-part` 下的通联服务对接流程说明

### 情况 B：暂时确认不到真实支付协议号来源

则后续执行：

- [ ] 固定为当前阻塞项
- [ ] 继续向业务 / 支付侧确认字段来源
- [ ] 暂不继续重复真联调

## 7. 后续文档事项

- [ ] 在 `docs/third-part` 下新增“通联服务对接流程”文档
- [ ] 明确通联签约、协议号获取、落库、供齐为使用的完整链路

## 8. 一句话 TODO

> 已确认真实 `payProtocolNo` 来自通联签约协议号，且已收到 mock 协议号 `AIP211926033187CF73483` 可继续联调；后续仍需补通联服务对接流程文档。

## 9. 本轮联调落地结果

`2026-04-02` 已完成基于 mock 协议号的最小改造与真实联调，结果如下：

1. 代码侧新增配置项：
   - `QW_MEMBER_SYNC_PAY_PROTOCOL_NO_OVERRIDE`
2. 当前逻辑调整为：
   - 若配置了 `memberSyncPayProtocolNoOverride`，则 `abs.push.order` 优先使用该值
   - 未配置时，仍沿用原有 `agreementNo / proto-` 回退逻辑
3. 真实联调日志保存在：
   - `/tmp/qw_real_push_and_token_20260401.log`
4. 本轮真实结果：
   - 首次请求出现一次传输层 `EOF reached while reading`
   - 随后重试同一笔单据时，`abs.push.order` 返回 `530 合作方订单号已经存在`
   - 同单 `abs.token.get` 返回 `200 OK`
   - 解密后已拿到可用 `token` 与 `redirectUrl`
5. 结论：
   - mock 协议号已经证明可打通齐为扣款 / 生效链路
   - 当前主阻塞已从“齐为协议或参数不明”进一步收敛为“正式环境必须接入通联获取真实支付协议号”
