#!/bin/bash

# ============================================================================
# 每日决策审查文档生成脚本
# ============================================================================
# 功能: 自动生成每日决策审查文档
# 使用: bash scripts/generate-daily-decision.sh
# ============================================================================

set -e

PROJECT_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
DECISION_DIR="$PROJECT_ROOT/docs/decision-review"
TODAY=$(date +%Y%m%d)
TODAY_DATE=$(date +%Y-%m-%d)
FILENAME="DAILY_DECISION_${TODAY}.md"
FILEPATH="$DECISION_DIR/$FILENAME"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ============================================================================
# 函数定义
# ============================================================================

print_header() {
    echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  每日决策审查文档生成器${NC}"
    echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
    echo ""
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# ============================================================================
# 主逻辑
# ============================================================================

print_header

# 检查 decision-review 目录
if [ ! -d "$DECISION_DIR" ]; then
    print_error "decision-review 目录不存在: $DECISION_DIR"
    exit 1
fi

print_info "项目根目录: $PROJECT_ROOT"
print_info "决策文档目录: $DECISION_DIR"
print_info "生成日期: $TODAY_DATE"
echo ""

# 检查文件是否已存在
if [ -f "$FILEPATH" ]; then
    print_warning "今日决策文档已存在: $FILENAME"
    echo ""
    read -p "是否要覆盖? (y/n) " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_info "操作取消"
        exit 0
    fi
fi

# 获取作者信息（可选）
if [ -z "$GIT_AUTHOR_NAME" ]; then
    GIT_AUTHOR_NAME=$(git config user.name 2>/dev/null || echo "Unknown")
fi

# 生成文件内容
cat > "$FILEPATH" << 'EOF'
# 每日决策审查

**日期**: DATE_PLACEHOLDER
**审查人**: AUTHOR_PLACEHOLDER
**状态**: 待完成

---

## 今日决策摘要

> 本日共进行 [N] 个决策

### 快速概览

| # | 决策标题 | 分类 | 优先级 | 状态 |
|---|---------|------|--------|------|
| 001 | [决策标题] | [架构/技术/业务/运维] | ⭐⭐ | ✅/⚠️/❌ |

---

## 决策详情

### DECISION-001: [决策标题]

**决策日期**: DATE_PLACEHOLDER
**决策人**: [角色/名字]
**所属分类**: [架构/技术/业务/运维/其他]
**优先级**: ⭐⭐⭐
**影响范围**: 中

#### 问题背景

描述今天遇到的问题或需求

#### 可选方案

| 方案 | 优势 | 劣势 | 评分 |
|------|------|------|------|
| 方案 A | | | ⭐⭐⭐⭐⭐ |
| 方案 B | | | ⭐⭐⭐⭐ |

#### 最终决策

**选择**: [方案名称]
**理由**: ...

#### 实施计划

- 阶段 1: [描述] (预计 X 天)
- 阶段 2: [描述] (预计 X 天)

#### 风险与缓解

| 风险 | 严重度 | 缓解方案 |
|------|--------|---------|
| | 中 | |

---

## 今日工作进度

### 已完成的工作

- ✅ [工作项]
- ✅ [工作项]

### 进行中的工作

- 🔄 [工作项] - XX% 完成度
- 🔄 [工作项] - XX% 完成度

### 遇到的问题

| 问题 | 严重度 | 解决方案 | 状态 |
|------|--------|---------|------|
| [问题] | 中 | [方案] | ✅ 已解决 |

---

## 与其他决策的关联

- 与 DECISION-XXX 关联 (前置/后置/并行)

---

**文档生成时间**: DATE_TIME_PLACEHOLDER
EOF

# 替换占位符
sed -i "s|DATE_PLACEHOLDER|$TODAY_DATE|g" "$FILEPATH"
sed -i "s|AUTHOR_PLACEHOLDER|$GIT_AUTHOR_NAME|g" "$FILEPATH"
sed -i "s|DATE_TIME_PLACEHOLDER|$TODAY_DATE $(date +%H:%M)|g" "$FILEPATH"

print_success "已生成: $FILENAME"
echo ""

# 显示文件信息
print_info "文件路径: $FILEPATH"
print_info "文件大小: $(wc -l < "$FILEPATH") 行"
echo ""

# 提示后续步骤
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo "后续步骤:"
echo "1. 编辑文件:"
echo -e "   ${GREEN}vim $FILEPATH${NC}"
echo ""
echo "2. 填写以下内容:"
echo "   - 今日的决策（标题、背景、方案、选择）"
echo "   - 工作进度（已完成、进行中）"
echo "   - 遇到的问题和解决方案"
echo ""
echo "3. 提交到 Git:"
echo -e "   ${GREEN}git add docs/decision-review/$FILENAME${NC}"
echo -e "   ${GREEN}git commit -m \"docs: daily decision review $TODAY_DATE\"${NC}"
echo ""
echo "4. 参考模板 (如需要):"
echo -e "   ${GREEN}cat docs/decision-review/DAILY_DECISION_TEMPLATE.md${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"

exit 0
