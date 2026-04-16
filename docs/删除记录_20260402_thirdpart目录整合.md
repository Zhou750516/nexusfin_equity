# 2026-04-02 `docs/thirdpart` 目录整合删除记录

## 1. 删除对象

本次计划删除的对象为：

1. `docs/thirdpart/齐为/20260401_齐为线上部署说明.md`
2. `docs/thirdpart/科技平台/20260401_科技平台线上部署说明.md`
3. 空目录 `docs/thirdpart/齐为`
4. 空目录 `docs/thirdpart/科技平台`
5. 空目录 `docs/thirdpart`

## 2. 删除原因

本次删除不是丢弃内容，而是进行目录口径整合：

1. 目前仓库中同时存在 `docs/thirdpart` 和 `docs/third-part`
2. 两套命名并存，容易导致文档维护分散和引用混乱
3. 用户已明确要求将 `docs/thirdpart` 下文件整合到 `docs/third-part` 中
4. 整合完成后，删除旧目录 `docs/thirdpart`

## 3. 是否可从 Git 恢复

可以。

说明：

1. 相关文件仍受 Git 管理
2. 即使迁移或误删，后续也可通过 Git 历史恢复

## 4. 替代位置

整合后的目标位置为：

1. `docs/third-part/齐为/20260401_齐为线上部署说明.md`
2. `docs/third-part/科技平台/20260401_科技平台线上部署说明.md`

## 5. 操作原则

1. 先迁移文件
2. 再更新仓库内引用
3. 最后删除旧目录 `docs/thirdpart`

## 6. 一句话说明

> 本次删除属于“目录归并后的清理动作”，不删除文档内容，只统一第三方文档目录口径为 `docs/third-part`。
