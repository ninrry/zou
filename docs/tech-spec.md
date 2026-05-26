# Tech Spec - 安卓原生技术方案

## 1. 技术栈强约束
必须使用：
- 语言：Kotlin
- UI：Jetpack Compose
- 架构：MVVM + Clean Architecture（轻量化）
- DI：Hilt
- 导航：Navigation Compose
- 本地数据库：Room
- 异步：Kotlin Coroutines + Flow
- 后台任务：WorkManager
- 精确提醒兜底：AlarmManager（必要时）
- 序列化：kotlinx.serialization
- 图片选择：Android Photo Picker / SAF
- Markdown 渲染：首版可选轻量方案，自研简单渲染或接入稳定库
- 测试：JUnit + Coroutine Test + Compose UI Test

## 2. 分层建议
### app
应用入口、导航、主题、依赖注入聚合

### core
- common
- ui
- designsystem
- time
- notification
- backup

### feature
- today
- tasks
- habits
- notes
- settings
- trash

### data
- database
- repository
- local datasource
- backup datasource

### domain
- model
- repository interface
- usecase

## 3. 架构原则
- UI 状态单向流动
- ViewModel 输出 StateFlow
- Repository 负责聚合数据与调度
- UseCase 保持轻量，但关键逻辑要封装
- 提醒调度逻辑集中管理，不分散在页面里

## 4. 数据持久化
数据库：Room

要求：
- 实体与 UI 模型解耦
- 需要 migration 预案
- 所有删除默认软删除
- 图片路径与媒体引用要可备份恢复

## 5. 提醒系统设计
目标支持：
- 开始提醒
- 特别提醒
- 重复提醒
- 免打扰
- 截止时间状态
- 完成后取消提醒

建议实现：
- 数据层保存提醒规则
- ReminderScheduler 统一生成下一次提醒
- WorkManager 处理常规调度
- 对必须精确的提醒，使用 AlarmManager 兜底
- 通知点击回到对应详情页

注意事项：
- 国产 ROM 对后台任务限制严格
- 需要处理系统重启后重新注册提醒
- 需要处理用户修改时间、时区变化

## 6. 图片与 Markdown
首版务实方案：
- 正文用 Markdown 字符串存储
- 图片保存为本地 app 专属目录文件
- 正文内用自定义占位格式引用图片，例如：
  `![image](local://media/xxx.jpg)`

编辑器方案建议：
- 普通文本编辑 + Markdown 辅助工具栏
- 插入图片时自动生成 Markdown 引用
- 详情页用 Markdown 渲染显示

## 7. 备份恢复方案
导出：
- 导出 JSON
- 导出媒体目录
- 统一 zip 打包

导入：
- 解析 JSON
- 恢复数据库
- 恢复媒体文件
- 做版本号字段，便于后续兼容

## 8. 权限建议
尽量减少权限：
- 通知权限（Android 13+）
- 精确闹钟权限（如必要）
- 文件读写通过 SAF / 系统选择器处理

## 9. 性能与稳定性
- 避免一次性做重渲染
- 大文本编辑要注意状态管理
- 图片加载使用成熟库，如 Coil
- 通知与数据库操作避免阻塞主线程
