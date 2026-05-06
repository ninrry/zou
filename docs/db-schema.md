# DB Schema - 数据模型与数据库设计

## 1. 统一建模思路
任务、习惯、笔记虽然业务不同，但共享很多基础字段。  
建议用“基础表 + 专属表”或者“独立表但共享字段设计规范”的方式实现。

首版可采用 **独立表 + 统一规范**，实现简单。

## 2. 通用字段规范
所有主实体建议包含：
- id: String
- title: String
- contentMarkdown: String?
- createdAt: Long
- updatedAt: Long
- isDeleted: Boolean
- deletedAt: Long?
- tags: String / JSON
- archived: Boolean

## 3. TaskEntity
字段建议：
- id
- title
- contentMarkdown
- priority
- isUrgent
- status
- startRemindAt
- remindWindowEndAt
- dueAt
- repeatIntervalMinutes
- exactReminderTimesJson
- allDay
- completionRule
- createdAt
- updatedAt
- isDeleted
- deletedAt

### 子任务 SubTaskEntity
- id
- taskId
- title
- sortOrder
- isCompleted
- completedAt
- createdAt
- updatedAt
- isDeleted

## 4. HabitEntity
- id
- title
- contentMarkdown
- frequencyType
- frequencyValueJson
- remindWindowStart
- remindWindowEnd
- repeatIntervalMinutes
- exactReminderTimesJson
- checkInMode
- targetDurationMinutes
- streakCountCache
- createdAt
- updatedAt
- isDeleted
- deletedAt

### HabitStepEntity
- id
- habitId
- title
- sortOrder
- createdAt
- updatedAt
- isDeleted

### HabitRecordEntity
- id
- habitId
- recordDate
- status
- durationMinutes
- createdAt
- updatedAt

## 5. NoteEntity
- id
- title
- contentMarkdown
- previewText
- createdAt
- updatedAt
- lastOpenedAt
- isDeleted
- deletedAt

## 6. MediaEntity
- id
- ownerType (task / habit / note)
- ownerId
- localPath
- mimeType
- sizeBytes
- createdAt
- updatedAt
- isDeleted

## 7. SettingsEntity / Preference 存储
建议 DataStore 存：
- 主题
- 免打扰开始时间
- 免打扰结束时间
- 默认提醒间隔
- 首选首页布局
- 备份版本号等轻量配置

## 8. 关键索引建议
- Task: dueAt, isUrgent, isDeleted, status
- HabitRecord: habitId + recordDate
- Note: updatedAt, isDeleted
- Media: ownerType + ownerId

## 9. 软删除规则
所有删除默认：
- isDeleted = true
- deletedAt = now

回收站展示：
- 所有 isDeleted = true 的实体

彻底删除时：
- 真删除实体
- 真删除关联媒体文件

## 10. 备份 JSON 建议结构
```json
{
  "version": 1,
  "exportedAt": 0,
  "tasks": [],
  "subTasks": [],
  "habits": [],
  "habitSteps": [],
  "habitRecords": [],
  "notes": [],
  "media": [],
  "settings": {}
}
```
