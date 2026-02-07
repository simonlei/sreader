# 📖 SReader — Android 纯文本阅读器

SReader 是一款 Android 平台的纯文本阅读器应用，专注于为用户提供舒适、高效的中文文本阅读体验。应用采用现代 Android 技术栈开发，支持多种中文编码格式，提供丰富的阅读定制选项，并具备智能的阅读进度管理和统计功能。

## ✨ 功能特性

### 📂 文件打开与编码支持
- 通过系统文件选择器（SAF）选择并打开文本文件
- 自动检测文件编码格式（支持 UTF-8、GBK、GB2312、GB18030、Big5 等常见中文编码）
- 编码检测失败时支持手动选择编码
- 大文件（>10MB）异步加载，3 秒内完成首页渲染

### 📄 分页阅读与翻页交互
- 基于 `StaticLayout` 的高精度文本分页引擎
- 支持左右滑动手势翻页
- 支持点击屏幕左右区域翻页、中央区域唤出工具栏
- 页面底部显示"当前页 / 总页数"信息
- 进度拖动条实现快速跳页

### 🎨 界面定制
- **字体大小**：12sp ~ 36sp 可调，步进 2sp
- **行间距**：紧凑 (1.0x) / 标准 (1.5x) / 宽松 (2.0x) 三档可选
- **背景主题**：白色、米黄、浅绿、深灰/夜间等多种预设主题
- **护眼模式**：暖色滤镜叠加，减少蓝光
- **亮度调节**：应用内独立亮度控制，不影响系统全局
- **屏幕常亮**：阅读时自动保持屏幕常亮
- 所有设置自动持久化，下次启动恢复

### 📑 目录识别与跳转
- 自动识别章节结构（"第X章"、"第X节"、"Chapter X"、纯数字编号等）
- 侧边栏目录面板，点击直达对应章节
- 当前章节高亮标识

### 🔍 全文搜索
- 顶部搜索栏输入关键词
- 匹配结果高亮显示
- 上一个 / 下一个导航，显示匹配数量（如"3/15"）

### 📚 阅读记录与历史管理
- 自动保存每本书的阅读位置（精确到字符偏移量）
- 再次打开自动跳转到上次阅读位置
- 首页按最近阅读时间排序展示历史记录
- 长按支持删除记录
- 自动过滤已删除或不可访问的文件

### 📊 阅读时长统计
- 后台精确记录阅读时长，关联到具体文件
- 离开阅读界面或应用进入后台自动暂停计时
- 按日 / 周 / 月多维度统计
- 柱状图可视化展示
- 每本书累计阅读时长排行

## 🛠️ 技术栈

| 技术 | 版本 |
|------|------|
| **Kotlin** | 2.2.10 |
| **AGP (Android Gradle Plugin)** | 9.0.0 |
| **Jetpack Compose** (BOM) | 2024.12.01 |
| **Material3** | Compose Material3 |
| **Room** | 2.7.1 |
| **KSP** | 2.2.10-2.0.2 |
| **minSdk** | 26 (Android 8.0) |
| **targetSdk** | 35 |

**架构模式**：Single Activity + Jetpack Navigation + MVVM

## 📁 项目结构

```
app/src/main/java/com/simon/sreader/
├── MainActivity.kt              # 主入口 + 导航编排
├── Screen.kt                    # 页面路由定义
├── core/
│   ├── EncodingDetector.kt      # BOM + UTF8 验证 + GBK/GB18030/Big5 检测
│   ├── FileLoader.kt            # SAF URI 文件加载
│   ├── TextPaginator.kt         # StaticLayout 分页引擎
│   ├── TocParser.kt             # 正则章节目录识别
│   ├── TextSearchEngine.kt      # 全文搜索引擎
│   └── ReadingTimer.kt          # 阅读计时器
├── data/
│   ├── AppDatabase.kt           # Room 数据库单例
│   ├── BookRecord.kt            # 书籍记录 Entity
│   ├── BookRecordDao.kt         # 书籍 DAO
│   ├── ReadingStats.kt          # 阅读时长 Entity
│   ├── ReadingStatsDao.kt       # 统计 DAO
│   ├── UserSettings.kt          # 用户设置 Entity
│   └── UserSettingsDao.kt       # 设置 DAO
└── ui/
    ├── home/
    │   ├── HomeScreen.kt        # 首页（历史记录 + 文件选择）
    │   └── HomeViewModel.kt
    ├── reader/
    │   ├── ReaderScreen.kt      # 阅读界面（手势 + 护眼 + 工具栏）
    │   ├── ReaderViewModel.kt
    │   ├── SettingsPanel.kt     # 设置面板（字体/行距/背景/亮度）
    │   ├── TocPanel.kt          # 目录面板
    │   └── SearchBar.kt         # 搜索栏
    ├── stats/
    │   ├── StatsScreen.kt       # 统计页（柱状图 + 排行）
    │   └── StatsViewModel.kt
    └── theme/
        ├── Theme.kt             # Compose 主题
        └── ReadingColors.kt     # 阅读背景色主题
```

## 🚀 构建与运行

### 环境要求

- **Android Studio**: Ladybug 或更高版本（需支持 AGP 9.0）
- **JDK**: 17+
- **Android SDK**: API 35

### 构建步骤

1. 克隆项目：
   ```bash
   git clone <repository-url>
   cd SReader
   ```

2. 使用 Android Studio 打开项目，等待 Gradle 同步完成。

3. 连接 Android 设备或启动模拟器（API 26+）。

4. 点击 **Run** 或通过命令行构建：
   ```bash
   ./gradlew assembleDebug
   ```

5. 安装 APK：
   ```bash
   ./gradlew installDebug
   ```

## 📱 使用说明

1. 启动应用后，点击首页的 **"+"** 按钮通过系统文件选择器选择 `.txt` 文本文件。
2. 文件加载完成后自动进入阅读界面，左右滑动或点击屏幕两侧翻页。
3. 点击屏幕中央区域可唤出顶部工具栏和底部设置面板。
4. 在设置面板中可调节字体大小、行间距、背景色、亮度、护眼模式等。
5. 工具栏提供目录、搜索等功能入口。
6. 退出阅读界面后，阅读进度自动保存，下次打开同一文件时自动恢复。
7. 首页底部可进入阅读统计页面，查看阅读时长数据。

## 📄 许可证

本项目仅供学习和个人使用。
