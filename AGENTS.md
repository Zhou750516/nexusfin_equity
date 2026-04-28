# NexusFin - nexusfin-equity

## Project Summary

惠聚项目艾博生权益分发服务，包含 Java 后端与 `H5/` 移动端前端。

## Read Order

1. `docs/plan/README.md`
2. `docs/plan/CURRENT_STATE.md`
3. `docs/plan/20260428.md`
4. `docs/plan/20260428_checklist.md`
5. `docs/plan/20260427_重构降摩擦.md`
6. `docs/AGENTS_BACKEND.md`
7. `docs/AGENTS_H5.md`

## Non-Negotiable Rules

- Controller 层不写业务逻辑
- 禁止硬编码配置值
- 禁止 `System.out.println`
- 禁止循环内查库
- 禁止吞异常

## Verification Baseline

- Java: `mvn test`
- H5: `cd H5 && ./node_modules/.bin/tsc --noEmit`
