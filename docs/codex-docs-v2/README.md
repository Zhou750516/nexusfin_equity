# Codex Docs v2 — 适配 nexusfin-equity 项目结构

> 本文档包适配您的 Java 后端 + H5 前端项目结构。

## 文件放置方式

```
nexusfin-equity/                    ← 项目根目录
├── AGENTS.md                       ← 原有的后端 AGENTS.md
│                                      ↑ 将 AGENTS_H5_APPEND.md 的内容追加到此文件末尾
├── H5/                             ← H5 前端目录
│   ├── PLAN.md                     ← 放这里
│   ├── API_SCHEMA.md               ← 放这里
│   ├── client/
│   │   └── src/
│   │       └── types/
│   │           └── loan.types.ts   ← TYPES.ts 复制到这里并改名
│   └── ...
├── src/                            ← Java 后端源码
├── pom.xml
└── ...
```

## 操作步骤

### 第 1 步：合并 AGENTS.md

打开项目根目录的 `AGENTS.md`，在文件末尾追加 `AGENTS_H5_APPEND.md` 的全部内容：

```bash
cat AGENTS_H5_APPEND.md >> AGENTS.md
```

### 第 2 步：放置其他文件

```bash
cp PLAN.md H5/PLAN.md
cp API_SCHEMA.md H5/API_SCHEMA.md
cp TYPES.ts H5/client/src/types/loan.types.ts
```

### 第 3 步：在 Codex 中使用

**全流程执行：**
```
Read AGENTS.md and H5/PLAN.md, then execute from Phase 0.
```

**只做前端改造（Phase 4-8）：**
```
Read AGENTS.md and H5/PLAN.md. I've completed Phase 0-3. Execute Phase 4 to refactor H5/client/src/pages/CalculatorPage.tsx.
```

**只做后端接口（Phase 9）：**
```
Read AGENTS.md, H5/PLAN.md, and H5/API_SCHEMA.md. Execute Phase 9 to implement Java backend APIs.
```

**单个任务：**
```
Read AGENTS.md. Add a numeric keypad drawer to H5/client/src/pages/CalculatorPage.tsx for the "修改金额" button. Use shadcn Drawer. Amount range: 100-5000, step 100.
```

## 文件清单

| 文件 | 放置位置 | 说明 |
|---|---|---|
| `AGENTS_H5_APPEND.md` | 追加到根目录 `AGENTS.md` 末尾 | H5 前端项目上下文、技术栈、目录结构、设计 Token、编码规范 |
| `PLAN.md` | `H5/PLAN.md` | 12 阶段执行计划，含任务清单和验收标准 |
| `API_SCHEMA.md` | `H5/API_SCHEMA.md` | 10 个 REST API 完整规范 + 数据库 Schema |
| `TYPES.ts` | `H5/client/src/types/loan.types.ts` | TypeScript 类型定义，含 Java DTO 对应注释 |
| `README.md` | 仅供参考，无需放入项目 | 本说明文件 |
