# 架构说明

本文描述 Zou 当前的代码结构和关键运行流程。具体实现以代码和自动化测试为准。

## 产品边界

Zou 是一个本地优先的 Android 应用，包含今日概览、待办、习惯、Markdown 笔记、提醒、回收站和备份恢复。

当前不包含云同步、团队协作、Web 或 iOS 客户端。

## 代码结构

```text
app/src/main/java/luzzr/zou/
├── app/        # 应用入口、顶层页面和导航
├── core/       # 通用 UI、主题、提醒、Markdown、权限和平台适配
├── data/       # Room、DataStore、备份、媒体和 Repository 实现
├── domain/     # 领域模型、Repository 接口和 UseCase
└── feature/    # today、tasks、habits、notes、settings、backup、trash
```

应用采用单模块、分层组织：

1. Compose 页面将用户操作交给 ViewModel。
2. ViewModel 通过 UseCase 调用领域能力，并以 `StateFlow` 暴露界面状态。
3. Repository 负责数据库、设置、媒体文件和提醒调度。
4. Room、DataStore、WorkManager 与 AlarmManager 处理本地持久化和后台工作。

依赖通过 Hilt 注入。

## 导航与界面

顶层页面包括今日、待办、习惯和笔记。它们位于同一个顶层画布中，通过分页和统一 Tab 切换。详情、编辑、设置、备份和回收站由 Navigation Compose 管理。

动效参数集中在 `core/ui`，页面使用语义化 motion token，避免各自维护不同的时长和缓动参数。

## 数据

Room 数据库当前版本为 `5`，包含以下实体：

- `TaskEntity` 与 `SubTaskEntity`
- `HabitEntity`、`HabitStepEntity` 与 `HabitRecordEntity`
- `NoteEntity`
- `MediaEntity`

用户设置存储在 DataStore。笔记图片保存在应用私有目录，正文通过本地媒体 ID 引用图片。

涉及多个表的写操作使用 Room 事务。文件写入无法与数据库共享事务，因此 Repository 会在失败时执行补偿清理，并在数据库提交后再删除不再需要的文件。

## 提醒

任务和习惯的提醒规则由 Repository 保存，提醒调度集中在 `core/reminder`：

- WorkManager 处理可延迟的后台工作。
- AlarmManager 处理需要精确触发的提醒，并在权限不可用时使用非精确兜底。
- 系统重启、应用更新、日期、时间和时区变化后会重新安排有效提醒。
- Android 13 及以上版本需要通知权限。

## 备份

备份格式当前版本为 `3`。导出文件是 ZIP，包含：

- `backup.json`
- 备份中引用的媒体文件

导入时，Room 数据在事务中按 ID 和 `updatedAt` 合并。设置恢复和提醒重排在数据库提交后执行；它们失败时会作为警告返回，不会错误地报告数据库已回滚。

## 重要约束

- 用户数据默认保存在设备本地。
- 删除默认进入回收站；彻底删除才移除数据库记录和相关媒体。
- 提醒调度逻辑不放在 Compose 页面中。
- 数据库 schema 变更必须提供迁移，并更新 `app/schemas`。
- 协程取消必须继续向上传播，不能转换为普通业务失败。
