# Android 性能验收手册

Zou 的动效与页面串联已经进入“需要可复跑证据”的阶段。主观视觉检查仍然重要，但每次大改 UI、动画、导航、依赖或构建工具链后，都应该至少保留一份快速性能冒烟记录。

## 快速帧率冒烟

前置条件：

- 至少一台 `adb devices` 显示为 `device` 的模拟器或真机
- 已安装 Android SDK 37
- Windows PowerShell

运行：

```powershell
.\scripts\android-gfxinfo-smoke.ps1
```

脚本会执行：

1. 构建 `:app:assembleDebug`
2. 按设备 ABI 安装 `app-x86_64-debug.apk` 或 `app-arm64-v8a-debug.apk`
3. 启动 `luzzr.zou`
4. 重置 `dumpsys gfxinfo`
5. 通过 UIAutomator 依次点击 `待办 -> 习惯 -> 笔记 -> 今日 -> 待办`
6. 保存 `gfxinfo`、`framestats`、最近 logcat 和 UI tree

输出目录：

```text
scratch/performance/<yyyyMMdd-HHmmss>/
```

其中 `summary.md` 是给人看的摘要，`summary.json` 适合后续脚本化比较。`scratch/` 已被 `.gitignore` 排除，不会污染提交。

## 读取结果

优先看这些字段：

- `Coordinate mode`：`ui-tree` 表示坐标来自 UIAutomator；`screen-fallback` 表示 UI 树不可用时按屏幕比例兜底。
- `Sample status`：`ok` 才适合作为快速趋势参考；`too_small` 只证明流程可达，不证明性能达标。
- `Total frames`：样本量太少时不要下结论
- `Janky percent`：作为趋势比较，不把单次模拟器结果当绝对真值
- `90th/95th/99th percentile`：观察 tab 切换和页面过渡是否有尖峰
- `Slow UI thread` / `Slow draw commands`：如果上升明显，再抓 Perfetto

模拟器帧数据会受宿主机负载、安装后的系统广播、UIAutomator 状态影响。一次异常只算线索；连续多次或同一流程前后对比才适合作为优化依据。

## 需要 Perfetto 的情况

如果快速冒烟出现以下情况，应进一步抓 Perfetto：

- tab 切换或 FAB 展开时 `Janky percent` 明显升高
- `Slow UI thread` 或 `Slow draw commands` 非零且持续出现
- 肉眼看到页面返回闪烁、列表删除卡顿、编辑步骤切换停顿
- 新增大规模图片、Markdown、备份恢复、数据库迁移相关逻辑

推荐聚焦单个流程，不要一次录“随便用一会儿”。例如：

- 顶层 tab 连续切换
- 今日页快速创建 FAB 展开和收起
- 任务完成/删除滑动
- 笔记编辑、预览和返回
- 设置页进入回收站/备份页再返回

## 验收基线

当前项目的最低性能回归习惯：

- UI/动画/导航大改后运行 `android-gfxinfo-smoke.ps1`
- 如果结果有尖峰，再用 Perfetto 或 Android Studio System Trace 定位
- 质量提交的最终验证仍包括：

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```

Release 打包在本机如果被外部模拟器服务锁住旧输出目录，可以用临时 build dir 验证，但 CI 会跑标准 `:app:assembleRelease`。
