# AutoSkipAd 设计文档

## 概述

AutoSkipAd 是一个 Android 应用，通过 AccessibilityService 自动检测并点击广告右上角的"跳过"按钮，实现自动跳过开屏广告。

## 架构

```
MainActivity.kt          — UI 界面，显示服务状态，引导用户开启无障碍服务
SkipAdService.kt         — 核心服务，监听屏幕变化，检测并点击跳过按钮
skip_service_config.xml  — 无障碍服务配置，声明监听的事件类型
```

## 检测逻辑

### 触发机制

SkipAdService 监听以下三种 AccessibilityEvent：

| 事件类型 | 说明 |
|---------|------|
| TYPE_WINDOW_STATE_CHANGED | 窗口切换（如打开新 App） |
| TYPE_WINDOW_CONTENT_CHANGED | 窗口内容变化（广告加载完成） |
| TYPE_VIEW_SCROLLED | 视图滚动 |

每次事件触发时，获取当前窗口的根节点（rootInActiveWindow），遍历节点树查找跳过按钮。

### 搜索区域

只在屏幕**右上角区域**搜索：
- 水平：右 50%（screenWidth * 0.5 ~ screenWidth）
- 垂直：上 35%（0 ~ screenHeight * 0.35）

不在该区域内的节点会被直接跳过，减少无效遍历。

### 关键词匹配

节点的 `text` 和 `contentDescription` 拼接后，检查是否包含以下关键词（不区分大小写）：

```
跳过、跳過、skip、Skip、SKIP
```

### 点击策略

1. **节点本身可点击** → 直接调用 `performAction(ACTION_CLICK)`
2. **节点不可点击** → 向上遍历父节点，找到第一个可点击的父节点并点击
3. 点击成功后立即返回，不再继续遍历

### 节点遍历

采用深度优先遍历（DFS），递归遍历每个节点的子节点。每个节点访问后调用 `recycle()` 释放资源，避免内存泄漏。

## 工作流程

```
用户打开 App
    ↓
点击"开启服务" → 跳转到系统无障碍设置
    ↓
用户手动开启 AutoSkipAd 无障碍服务
    ↓
SkipAdService 启动，记录屏幕尺寸
    ↓
监听 AccessibilityEvent
    ↓
事件触发 → 获取根节点 → 遍历右上角区域
    ↓
找到匹配关键词的可点击节点 → 执行点击
    ↓
广告被跳过
```

## 文件结构

```
AutoSkipAd/
├── app/src/main/java/com/autoskip/
│   ├── MainActivity.kt          # 主界面
│   └── SkipAdService.kt         # 无障碍服务
├── app/src/main/res/
│   ├── layout/activity_main.xml # 主界面布局
│   ├── xml/skip_service_config.xml # 服务配置
│   └── values/strings.xml       # 字符串资源
├── .github/workflows/build.yml  # CI 构建配置
└── build.gradle.kts             # 项目构建配置
```

## 技术栈

- 语言：Kotlin
- 最低 API：24（Android 7.0）
- 目标 API：34（Android 14）
- 构建工具：Gradle 8.2 + AGP 8.2.0
- CI：GitHub Actions（Ubuntu + JDK 17）

## 已知限制

1. 只检测右上角区域，部分广告的跳过按钮位置可能不在范围内
2. 关键词列表有限，部分 App 使用其他文字（如"关闭"、"×"）无法识别
3. 依赖事件触发，如果广告出现到消失的时间极短，可能错过
4. 部分 App 的跳过按钮可能不可点击或需要特殊交互方式
