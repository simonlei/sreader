# SReader 实施计划

## 技术栈
- **Kotlin + Jetpack Compose + Material3**（UI 层）
- **Room + KSP**（本地数据持久化）
- **Coroutines + Flow**（异步处理，Kotlin 自带）
- **无额外框架**（不用 Hilt、不用 Navigation-Compose、不用第三方图表库）

## 包名：`com.simon.sreader`

---

- [ ] 1. 项目基础配置
  - 在 `gradle/libs.versions.toml` 中声明依赖版本（Compose BOM、Material3、Room、KSP）
  - 修改 `app/build.gradle.kts`：启用 Compose、KSP 插件，添加 Room 依赖
  - 将 `minSdk` 从 34 降至 26（兼容更多设备）
  - 创建 `MainActivity.kt`（Single Activity，Compose 入口），删除自动生成的旧模板代码
  - 用一个简单的状态变量管理页面切换（首页 / 阅读页 / 统计页），不引入 Navigation 库
  - _需求：非功能性需求 3（兼容性）、非功能性需求 4（架构）_

- [ ] 2. Room 数据库 + 数据模型
  - 创建 `data/AppDatabase.kt`（Room 数据库，包含所有 Entity）
  - 创建 Entity：
    - `BookRecord`：文件 URI、文件名、编码格式、最后阅读时间、阅读位置（字符偏移量）、总字符数
    - `ReadingStats`：关联 BookRecord ID、日期（yyyy-MM-dd）、阅读时长（秒）
    - `UserSettings`：字体大小、行间距倍数、背景色主题、护眼模式、亮度值（单行记录）
  - 创建对应的 DAO（`BookRecordDao`、`ReadingStatsDao`、`UserSettingsDao`）
  - 使用全局单例提供数据库实例（`AppDatabase.getInstance(context)`）
  - _需求：4.1~4.7、9.1、2.5、3.5_

- [ ] 3. 文件打开与编码检测
  - 创建 `core/EncodingDetector.kt`：自实现编码检测（BOM 头检测 → 尝试 UTF-8 解码验证 → 回退 GBK），支持 UTF-8/GBK/GB2312/GB18030/Big5
  - 创建 `core/FileLoader.kt`：通过 SAF URI 读取文件内容，调用 EncodingDetector 解码为字符串
  - 在首页集成系统文件选择器（`ACTION_OPEN_DOCUMENT`，MIME type `text/*`），获取持久化 URI 权限
  - 打开文件后创建/更新 BookRecord 记录
  - _需求：1.1~1.4、4.4_

- [ ] 4. 文本分页引擎
  - 创建 `core/TextPaginator.kt`：根据显示区域尺寸、字体大小、行间距，使用 `TextPaint` + `StaticLayout` 将全文切分为页面列表
  - 每页记录起始/结束字符偏移量，支持通过偏移量反查页码
  - 后台协程执行分页计算，先快速渲染当前页，再异步完成全文分页
  - 显示设置变化时重新分页，根据字符偏移量保持阅读位置
  - _需求：5.1~5.4、非功能性需求 1（性能）_

- [ ] 5. 阅读主界面 + 翻页交互
  - 创建 `ui/reader/ReaderScreen.kt`：
    - 使用 Compose `Text` 或 Canvas 渲染当前页内容
    - 手势层：左侧/右侧点击 → 下一页，中央点击 → 切换工具栏显示；左滑 → 下一页，右滑 → 上一页
    - 页面底部显示"当前页/总页数"
    - 屏幕常亮
  - 创建 `ui/reader/ReaderViewModel.kt`：管理页码、文本内容、阅读位置保存（退出时 / 翻页时自动存储）
  - 工具栏（中央点击展开）：
    - 顶部：返回按钮、书名、搜索按钮、目录按钮
    - 底部：页码 Slider 拖动跳转
  - _需求：7.1~7.8、5.1~5.3_

- [ ] 6. 设置面板
  - 创建 `ui/reader/SettingsPanel.kt`（BottomSheet）：
    - 字体大小：当前值 + 加减按钮（12sp~36sp）
    - 行间距：紧凑 1.0x / 标准 1.5x / 宽松 2.0x 三档按钮
    - 背景色：横排色块（白色/米黄/浅绿/深灰），点击切换，自动适配文字颜色
    - 护眼模式：Switch 开关，叠加暖色 Overlay
    - 亮度调节：Slider，修改 `WindowManager.LayoutParams.screenBrightness`
  - 设置变更实时生效 + 持久化到 Room，背景/字体变化触发重新分页
  - _需求：2.1~2.5、3.1~3.5_

- [ ] 7. 首页：历史记录 + 文件入口
  - 创建 `ui/home/HomeScreen.kt`：
    - LazyColumn 展示历史阅读记录（书名、最后阅读时间、进度），按最近阅读时间倒序
    - 过滤不可访问的文件 URI
    - 空状态引导提示
    - FAB 按钮打开文件选择器
    - 长按删除记录
    - 点击记录跳转阅读页，恢复上次位置
  - 创建 `ui/home/HomeViewModel.kt`
  - _需求：4.3~4.7_

- [ ] 8. 目录识别与跳转
  - 创建 `core/TocParser.kt`：正则匹配章节标题（"第X章/节"、"Chapter X"、数字编号"1."/"1.1"等），返回标题+字符偏移量列表
  - 创建 `ui/reader/TocPanel.kt`（侧边抽屉或 BottomSheet）：展示目录、高亮当前章节、点击跳转
  - 无目录时隐藏入口按钮
  - _需求：6.1~6.5_

- [ ] 9. 搜索功能
  - 创建 `ui/reader/SearchBar.kt`：顶部搜索框 + 上一个/下一个导航 + 匹配计数
  - 创建 `core/TextSearchEngine.kt`：全文搜索关键词，返回匹配偏移量列表，大小写不敏感
  - 搜索结果高亮渲染（当前匹配醒目色，其他匹配淡色），关闭搜索恢复原位
  - _需求：8.1~8.6_

- [ ] 10. 阅读时长统计
  - 创建 `core/ReadingTimer.kt`：onResume 开始计时，onPause 暂停，每 30 秒写入数据库
  - 创建 `ui/stats/StatsScreen.kt`：
    - Tab 切换：按天 / 按周 / 按月
    - 使用 Compose Canvas 自绘简易柱状图（最近 7 天 / 4 周 / 12 个月）
    - 底部：每本书累计阅读时长排行列表
  - 创建 `ui/stats/StatsViewModel.kt`：查询聚合数据
  - _需求：9.1~9.5_
