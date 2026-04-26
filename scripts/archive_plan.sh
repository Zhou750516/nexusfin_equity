#!/usr/bin/env bash
# scripts/archive_plan.sh
# 归档 docs/plan/ 下超过 N 天的 plan 文件，按主题分流到 topics/ 或 archive/YYYYMM/。
# 默认 dry-run，加 --execute 才真的 git mv。
#
# 用法：
#   scripts/archive_plan.sh                    # dry-run，列出拟归档清单
#   scripts/archive_plan.sh --cutoff 14        # 改 14 天阈值
#   scripts/archive_plan.sh --execute          # 真的执行 git mv
#   scripts/archive_plan.sh --cutoff 14 --execute
#
# 设计：
#   - 只匹配 [0-9]{8} 开头的 plan 文件，不动 README.md / CURRENT_STATE.md / 已移到子目录的文件
#   - 主题归类按文件名关键字匹配（见 classify_topic 函数），未命中的回落到 archive/YYYYMM/
#   - 同一文件如果命中多个主题，按规则顺序取首个

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PLAN_DIR="$REPO_ROOT/docs/plan"
CUTOFF_DAYS=7
EXECUTE=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --execute)
            EXECUTE=true
            shift
            ;;
        --dry-run)
            EXECUTE=false
            shift
            ;;
        --cutoff)
            CUTOFF_DAYS="$2"
            shift 2
            ;;
        -h|--help)
            sed -n '2,/^$/p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
            exit 0
            ;;
        *)
            echo "Unknown arg: $1" >&2
            echo "Run with -h for help." >&2
            exit 2
            ;;
    esac
done

# Compute cutoff date in YYYYMMDD (BSD/macOS or GNU date)
if date -v -1d +%Y%m%d >/dev/null 2>&1; then
    CUTOFF_DATE=$(date -v -"${CUTOFF_DAYS}"d +%Y%m%d)
else
    CUTOFF_DATE=$(date -d "${CUTOFF_DAYS} days ago" +%Y%m%d)
fi

# Topic classification — order matters (more specific first).
# 返回主题目录名；空字符串表示无主题（回落到 archive/）。
classify_topic() {
    local fn="$1"
    case "$fn" in
        *项目阶段性*|*项目开发到上线*) echo "项目阶段性" ;;
        *齐为*|*通联*|*allinpay*|*abs.lending*|*abs.token*|*payProtocolNo*) echo "齐为" ;;
        *小花*|*联合登录*) echo "小花科技" ;;
        *异步补偿*|*异步消息补偿*|*自动调度*) echo "异步补偿" ;;
        *云卡*) echo "云卡" ;;
        *科技平台*|*techplatform*) echo "科技平台" ;;
        *艾博生*|*后端日志*) echo "后端日志" ;;
        *退款*|*分发页*) echo "退款分发" ;;
        *H5多语言*|*H5自动埋点*|*H5绑卡*|*H5_Phase9*|*H5到后端*) echo "H5" ;;
        *M3.1*|*M3.2*|*M2外部*|*对外同步*) echo "里程碑" ;;
        *) echo "" ;;
    esac
}

echo "=== Plan 归档计划 ==="
echo "Plan 目录:   $PLAN_DIR"
echo "Cutoff 阈值: $CUTOFF_DAYS 天（$CUTOFF_DATE 之前的文件会被归档）"
echo "执行模式:    $([ "$EXECUTE" = true ] && echo "EXECUTE（真的 git mv）" || echo "DRY-RUN（只打印不动文件）")"
echo ""

# Collect moves
declare -a MOVES=()
declare -a SKIPPED_RECENT=()
shopt -s nullglob
for f in "$PLAN_DIR"/[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]*.md "$PLAN_DIR"/[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]*.sh; do
    [ -f "$f" ] || continue
    bn=$(basename "$f")
    date_prefix=$(echo "$bn" | grep -oE '^[0-9]{8}' | head -1)
    [ -z "$date_prefix" ] && continue

    # Skip files newer than cutoff
    if [ "$date_prefix" -ge "$CUTOFF_DATE" ]; then
        SKIPPED_RECENT+=("$bn")
        continue
    fi

    yyyymm="${date_prefix:0:6}"
    topic=$(classify_topic "$bn")

    if [ -n "$topic" ]; then
        target="$PLAN_DIR/topics/$topic/$bn"
    else
        target="$PLAN_DIR/archive/$yyyymm/$bn"
    fi

    MOVES+=("$f|$target")
done
shopt -u nullglob

# Group + print（兼容 bash 3.x，不用关联数组）
echo "=== 拟归档：${#MOVES[@]} 个文件 ==="
echo ""

# 用临时文件做 group-by-destination，兼容 bash 3.2
TMP_GROUP=$(mktemp)
trap 'rm -f "$TMP_GROUP"' EXIT
for m in "${MOVES[@]}"; do
    src="${m%%|*}"
    dst="${m##*|}"
    rel_src="${src#$PLAN_DIR/}"
    rel_dst="${dst#$PLAN_DIR/}"
    dst_dir=$(dirname "$rel_dst")
    printf '%s\t%s\n' "$dst_dir" "$rel_src" >> "$TMP_GROUP"
done

for dir in $(awk -F'\t' '{print $1}' "$TMP_GROUP" | sort -u); do
    count=$(awk -F'\t' -v d="$dir" '$1==d' "$TMP_GROUP" | wc -l | tr -d ' ')
    echo "→ docs/plan/$dir/  ($count 个)"
    awk -F'\t' -v d="$dir" '$1==d {print "    " $2}' "$TMP_GROUP"
    echo ""
done

echo "=== 留在 docs/plan/ 根目录（最近 $CUTOFF_DAYS 天，不归档）==="
if [ ${#SKIPPED_RECENT[@]} -eq 0 ]; then
    echo "  (无)"
else
    printf '  %s\n' "${SKIPPED_RECENT[@]}" | sort -u
fi
echo ""

# Execute
if [ "$EXECUTE" = true ]; then
    if [ ${#MOVES[@]} -eq 0 ]; then
        echo "=== 无文件需要归档 ==="
        exit 0
    fi

    echo "=== 执行 git mv ==="
    for m in "${MOVES[@]}"; do
        src="${m%%|*}"
        dst="${m##*|}"
        mkdir -p "$(dirname "$dst")"
        if [ -e "$dst" ]; then
            echo "  SKIP（目标已存在）: $dst"
            continue
        fi
        git mv "$src" "$dst"
        echo "  ✓ $(basename "$src") → ${dst#$PLAN_DIR/}"
    done
    echo ""
    echo "=== 完成 ==="
    echo "记得 git status 检查后 commit："
    echo "  git status docs/plan"
    echo "  git commit -m 'chore: archive aged plan files into topics/ and archive/'"
else
    echo "=== DRY-RUN 完成 ==="
    echo "确认无误后用 --execute 真的执行："
    echo "  scripts/archive_plan.sh --execute"
fi
