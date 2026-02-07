# 实施计划

- [ ] 1. 重构 `EncodingDetector`，支持从 `InputStream` 独立检测编码并返回 BOM 长度
   - 新增 `detect(inputStream): Pair<String, Int>` 方法，读取前 8KB 进行编码检测，同时返回编码名称和 BOM 字节长度
   - 保留现有 `detect(bytes)` 方法不变，保持向后兼容
   - _需求：1.1_

- [ ] 2. 重构 `FileLoader.loadFile()`，实现流式读取和进度回调
   - 拆分加载流程为两阶段：第一阶段先 `openInputStream` 读取前 8KB 做编码检测；第二阶段用检测到的编码创建 `BufferedReader` 流式读取全文，避免先 `readBytes()` 再转 `String` 的双倍内存占用
   - 新增带进度回调的重载方法 `loadFile(context, uri, onProgress: (phase: String, progress: Float) -> Unit)`，在读取过程中按已读字节 / 文件总大小报告进度
   - 通过 `contentResolver.query()` 获取文件大小 `OpenableColumns.SIZE`，用于计算读取进度百分比
   - _需求：1.1、1.2、1.3、4.1、4.2_

- [ ] 3. 优化 `TextPaginator.paginate()`，消除 O(n²) 的 `substring` 问题
   - 将 `text.substring(currentOffset)` 替换为 `StaticLayout.Builder.obtain(text, currentOffset, end, ...)` 的偏移量方式，直接在原始 `CharSequence` 上操作
   - 为每次 `StaticLayout` 测量引入窗口限制：从 `currentOffset` 开始，仅取 `min(currentOffset + windowSize, text.length)` 范围的文本进行布局测量，`windowSize` 可设为当前页预估字符数的 3~5 倍
   - 若窗口内不足一页内容，则逐步扩大窗口直到覆盖至少一页，确保分页结果与优化前一致
   - _需求：2.1、2.2、2.3_

- [ ] 4. 为 `TextPaginator.paginate()` 添加分页进度回调
   - 新增带进度回调的重载方法 `paginate(..., onProgress: (Float) -> Unit)`，按 `currentOffset / text.length` 报告分页进度
   - 回调频率控制在每处理 1% 或每 100 页触发一次，避免过于频繁的 UI 更新
   - _需求：3.2_

- [ ] 5. 扩展 `ReaderUiState`，增加加载阶段和进度信息字段
   - 在 `ReaderUiState` 中新增 `loadingPhase: String`（如"正在读取文件..."、"正在排版..."）和 `loadingProgress: Float`（0f ~ 1f）字段
   - 默认值分别为 `""` 和 `0f`，在加载完成后重置
   - _需求：3.1、3.2_

- [ ] 6. 更新 `ReaderViewModel` 的加载和分页流程，串联进度报告
   - 在 `loadBook()` 中调用带进度回调的 `FileLoader.loadFile()`，将读取进度映射到 `ReaderUiState.loadingPhase = "正在读取文件..."` 和 `loadingProgress`
   - 在 `repaginate()` 中调用带进度回调的 `TextPaginator.paginate()`，将分页进度映射到 `loadingPhase = "正在排版..."` 和 `loadingProgress`
   - _需求：3.1、3.2_

- [ ] 7. 更新 `ReaderScreen` 加载状态 UI，展示进度信息
   - 将 `isLoading` 分支中的纯 `CircularProgressIndicator` 替换为包含进度百分比文字和阶段描述的组合组件
   - 对于大文件（进度 > 0），显示确定性进度指示器（`CircularProgressIndicator(progress = ...)`）；对于小文件（进度 = 0），保留原有不确定性转圈动画
   - _需求：3.1、3.2、3.3_

- [ ] 8. 添加大文件内存安全检查
   - 在 `FileLoader.loadFile()` 中，加载前通过 `Runtime.getRuntime().maxMemory()` 和 `freeMemory()` 估算可用内存，若文件大小超过可用内存的 50%，通过回调或返回值提示警告信息
   - 在 `ReaderViewModel` 中接收该警告，通过 `ReaderUiState` 传递给 UI 层展示（如 Toast 或提示文本）
   - _需求：4.3_

- [ ] 9. 功能兼容性验证与回归修复
   - 确认 `TocParser`、`TextSearchEngine` 在新的加载流程下仍能正常获取 `fullText` 并正确工作
   - 确认 `goToOffset()`、`findPageByOffset()` 等阅读位置恢复逻辑在优化后的分页结果下行为一致
   - 确认 `onSettingsChanged()` 触发重新分页后，当前阅读位置能正确保持
   - 测试不同编码文件（UTF-8、GBK、GB18030、Big5、UTF-16LE/BE）的加载正确性
   - _需求：5.1、5.2、5.3、5.4、5.5_
