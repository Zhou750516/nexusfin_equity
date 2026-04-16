# 决策审查文档管理体系

## 概述

本目录存放项目的决策审查文档，包括：
- **完整决策审查** (DECISION_REVIEW_FULL_*.md) - 阶段性总结
- **每日决策审查** (DAILY_DECISION_*.md) - 日常更新

---

## 文件说明

### 1. DECISION_REVIEW_FULL_20260331.md (完整决策审查)

**更新频率**: 每周/重要阶段完成时
**包含内容**:
- 从项目开始至今的所有关键决策
- 每个决策的详细分析（背景、方案对比、理由）
- 决策矩阵总览
- 立即采取的行动清单

**使用场景**:
- 新团队成员了解项目历史
- 重要评审会（评审部）
- 项目总结汇报

---

### 2. DAILY_DECISION_*.md (每日决策审查)

**更新频率**: 每天
**包含内容**:
- 当日做出的重要决策
- 当日工作进度
- 遇到的问题和解决方案
- 对之前决策的评估反馈

**使用场景**:
- 早会/ 站会汇报
- 决策跟踪
- 工作日志

---

## 如何使用

### 方式 1: 手动创建（推荐日常使用）

每天 EOD（下班前）花 10-15 分钟：

```bash
# 1. 复制模板
cp DAILY_DECISION_TEMPLATE.md DAILY_DECISION_$(date +%Y%m%d).md

# 2. 编辑文件，填写今日决策和工作进度
vim DAILY_DECISION_$(date +%Y%m%d).md

# 3. 提交到 Git
git add docs/decision-review/
git commit -m "docs: daily decision review $(date +%Y-%m-%d)"
```

### 方式 2: 自动生成脚本（推荐）

#### A. 使用 Shell 脚本

创建文件 `scripts/generate-daily-decision.sh`:

```bash
#!/bin/bash

# 生成每日决策文档脚本
DATE=$(date +%Y%m%d)
FILENAME="docs/decision-review/DAILY_DECISION_${DATE}.md"

if [ -f "$FILENAME" ]; then
    echo "今日决策文档已存在: $FILENAME"
    exit 1
fi

# 复制模板
cp docs/decision-review/DAILY_DECISION_TEMPLATE.md "$FILENAME"

# 替换日期占位符
sed -i "s/YYYY-MM-DD/$(date +%Y-%m-%d)/g" "$FILENAME"

echo "✅ 已生成: $FILENAME"
echo "请编辑文件并提交: git add $FILENAME && git commit -m 'docs: daily decision review $(date +%Y-%m-%d)'"
```

使用方式：
```bash
bash scripts/generate-daily-decision.sh
```

---

#### B. 使用 Git Hook 自动触发

在 `.git/hooks/post-merge` 中添加：

```bash
#!/bin/bash

# 如果是新的一天，生成决策文档提醒
TODAY=$(date +%Y%m%d)
DECISION_FILE="docs/decision-review/DAILY_DECISION_${TODAY}.md"

if [ ! -f "$DECISION_FILE" ]; then
    echo ""
    echo "⚠️  提醒: 尚未创建今日决策审查文档"
    echo "运行: bash scripts/generate-daily-decision.sh"
    echo ""
fi
```

使用方式：
```bash
chmod +x .git/hooks/post-merge
# 之后每次 git pull 都会检查
```

---

#### C. 使用 Cron 定时任务（团队共享）

在项目根目录下创建 `scripts/daily-decision-cron.sh`:

```bash
#!/bin/bash

PROJECT_DIR="/path/to/nexusfin-equity"
cd "$PROJECT_DIR"

DATE=$(date +%Y%m%d)
FILENAME="docs/decision-review/DAILY_DECISION_${DATE}.md"

# 检查今日文件是否已存在
if [ ! -f "$FILENAME" ]; then
    # 生成文件
    cp docs/decision-review/DAILY_DECISION_TEMPLATE.md "$FILENAME"
    sed -i "s/YYYY-MM-DD/$(date +%Y-%m-%d)/g" "$FILENAME"

    # 自动提交
    git add "$FILENAME"
    git commit -m "chore: generate daily decision review $(date +%Y-%m-%d)" || true
fi
```

配置 Cron：

```bash
# 每天 08:30 生成文档（未来会填充）
30 8 * * * /path/to/scripts/daily-decision-cron.sh

# 每天 17:55 提醒填写（如果还没填）
55 17 * * * echo "提醒: 请填写今日决策审查文档" | mail -s "Daily Decision Review Reminder" team@example.com
```

配置方式：
```bash
crontab -e
```

---

## 工作流程

### 日常工作流（每天）

```
08:30  → Cron 自动生成当日决策审查文件 (模板)
09:00  → 团队站会，讨论今日计划和前日决策
...    → 全天工作
16:00  → 有重要决策时，实时更新文档
17:00  → 整理今日工作进度
17:30  → 编辑完成，添加具体内容
18:00  → 提交决策审查文档
        → 自动上传到知识库（可选）
```

### 周末工作流（每周五）

```
15:00  → 整理本周所有决策
15:30  → 生成本周决策总结（可选文件: WEEKLY_DECISION_SUMMARY_*.md）
16:00  → 确认所有决策已记录
16:30  → 准备周报时引用决策文档
```

### 阶段汇总（重要阶段）

```
当重要阶段完成时（如上线前）：
1. 聚合所有日常决策文档
2. 生成新的完整审查报告 (DECISION_REVIEW_FULL_*.md)
3. 提交给评审部（ENG_REVIEW）
4. 存档并为下个阶段重置
```

---

## 决策文档内容指南

### 什么是"决策"？

**应该记录** (✅):
- 技术方案选型（如：监控系统选 CloudMonitor）
- 架构设计变更（如：拆分服务，合并模块）
- API/数据库 Schema 变更
- 重要流程优化
- 风险识别和缓解方案

**不需要记录** (❌):
- 日常 Bug 修复
- 常规代码审查反馈
- 文档更新
- 依赖包升级（除非是重大版本）

### 决策的五要素

每个决策记录应包含：

1. **背景** - 为什么要做这个决策？
2. **方案** - 有哪些可选方案？
3. **选择** - 最终选择了哪个方案？为什么？
4. **计划** - 如何执行这个决策？
5. **风险** - 有什么风险，如何缓解？

---

## 与其他文档的关联

```
┌─ DECISION_REVIEW_FULL_*.md (完整决策汇总)
│  ├─ 引用所有 DAILY_DECISION_*.md
│  ├─ 定期上传给: ENG_REVIEW_*.md
│  └─ 评审部门会引用到: RISK_ASSESSMENT_*.md
│
├─ DAILY_DECISION_*.md (日常决策记录)
│  └─ 支撑: DECISION_REVIEW_FULL_*.md
│
├─ ENG_REVIEW_*.md (工程审查)
│  └─ 参考: DECISION_REVIEW_FULL_*.md
│
└─ 其他文档
   ├─ MONITORING_SYSTEM_DESIGN_*.md
   ├─ TROUBLESHOOTING_GUIDE_*.md
   └─ RISK_ASSESSMENT_*.md
```

---

## Git 提交约定

每次提交决策审查文档时，使用统一的 commit message:

```bash
# 每日决策
git commit -m "docs: daily decision review YYYY-MM-DD

- 决策 1: [简要描述]
- 决策 2: [简要描述]

Ref: DECISION-XXX, DECISION-YYY"

# 完整阶段审查
git commit -m "docs: decision review full - phase name

[详细描述]"
```

---

## 知识库集成（可选）

### 导出为 Wiki

每周将决策审查导出到团队 Wiki：

```bash
# 将 Markdown 转换为 Wiki 格式
pandoc DAILY_DECISION_*.md -t mediawiki -o decision-review.wiki

# 或上传到 Confluence
curl -X POST -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d @decision-review.json \
  https://confluence.company.com/rest/api/content
```

### 设置提醒

在 README 中添加最新的决策链接：

```markdown
## 最新决策审查

📅 **今天** - [DAILY_DECISION_20260331.md](decision-review/DAILY_DECISION_20260331.md)

📅 **本周摘要** - [周报](weekly-summary.md)

📅 **完整审查** - [DECISION_REVIEW_FULL_20260331.md](decision-review/DECISION_REVIEW_FULL_20260331.md)
```

---

## 常见问题

### Q: 一天内有多个决策怎么办？

**A**: 在同一个 DAILY_DECISION_*.md 文件中新增 DECISION 部分：

```markdown
# 每日决策审查

## 决策 1: ...
## 决策 2: ...
## 决策 3: ...
```

### Q: 决策需要时间讨论，需要多天吗？

**A**: 可以，在最终决策时再记录：

```markdown
### DECISION-XXX: [决策标题]

**讨论时间**: 2026-03-29 至 2026-03-31
**最终决策时间**: 2026-03-31
```

### Q: 如何检查历史决策？

**A**: 使用 Git 历史：

```bash
# 查看所有决策文件的变更历史
git log --oneline -- docs/decision-review/

# 查看某个决策的完整历史
git log -p -- docs/decision-review/DAILY_DECISION_20260331.md
```

### Q: 决策文档会变成什么样的大小？

**A**: 每日文档约 500-2000 行（取决于决策数量）：

```
- 1-2 个决策：500-800 行
- 3-5 个决策：800-1500 行
- 5+ 个决策：1500-2000 行
```

定期归档旧文档：

```bash
# 打包历史
tar -czf decision-review-archive-2026-q1.tar.gz docs/decision-review/DAILY_*.md
```

---

## 模板和脚本下载

本目录包含：
- ✅ `DAILY_DECISION_TEMPLATE.md` - 每日模板
- ✅ `DECISION_REVIEW_FULL_*.md` - 完整审查示例
- 📋 `scripts/generate-daily-decision.sh` - 生成脚本

---

## 最佳实践

### ✅ 做

- ✅ 每天花 10-15 分钟记录决策
- ✅ 及时记录，不要周末补写
- ✅ 决策前就记录背景，决策后更新选择和计划
- ✅ 写下理由，为未来的人解释为什么这样做
- ✅ 定期回顾，评估决策效果
- ✅ 从决策中学习，改进流程

### ❌ 不做

- ❌ 不要等到最后才突击补写
- ❌ 不要记录无关的日常工作
- ❌ 不要只写决策，不写理由
- ❌ 不要忘记评估之前的决策

---

## 支持和反馈

如有改进建议，请：
1. 在本 README 中添加新的问题
2. 改进模板或脚本
3. 提交 PR 或 issue

---

**文档最后更新**: 2026-03-31
**维护人**: SRE / 技术负责人
**下次审查**: 2026-04-07
