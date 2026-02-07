package com.simon.sreader.core

/**
 * 目录（Table of Contents）解析器
 * 通过正则匹配识别文本中的章节标题
 */
object TocParser {

    /**
     * 目录条目
     */
    data class TocEntry(
        val title: String,
        val charOffset: Int,
        val level: Int = 1 // 1=章, 2=节
    )

    // 匹配模式列表（按优先级排序）
    private val patterns = listOf(
        // 第X章、第X回、第X卷
        Regex("""^(第[零一二三四五六七八九十百千\d]+[章回卷部篇集].*)""", RegexOption.MULTILINE) to 1,
        // 第X节
        Regex("""^(第[零一二三四五六七八九十百千\d]+[节].*)""", RegexOption.MULTILINE) to 2,
        // Chapter X / CHAPTER X
        Regex("""^((?:Chapter|CHAPTER)\s+\d+.*)""", RegexOption.MULTILINE) to 1,
        // 数字编号: "1." "1.1" "第1章" 等（行首）
        Regex("""^(\d+\.\s+\S.*)""", RegexOption.MULTILINE) to 1,
        Regex("""^(\d+\.\d+\s+\S.*)""", RegexOption.MULTILINE) to 2,
        // 【xxx】格式标题
        Regex("""^(【.+?】.*)""", RegexOption.MULTILINE) to 1,
    )

    /**
     * 解析文本中的目录结构
     * @param text 全文文本
     * @return 目录条目列表（可能为空）
     */
    fun parse(text: String): List<TocEntry> {
        val entries = mutableListOf<TocEntry>()
        val usedOffsets = mutableSetOf<Int>() // 避免重复

        for ((pattern, level) in patterns) {
            val matches = pattern.findAll(text)
            for (match in matches) {
                val offset = match.range.first
                if (offset !in usedOffsets) {
                    val title = match.groupValues[1].trim()
                    // 标题不应太长（超过 50 字符可能不是标题）
                    if (title.length <= 50) {
                        entries.add(TocEntry(title, offset, level))
                        usedOffsets.add(offset)
                    }
                }
            }
        }

        // 按字符偏移量排序
        entries.sortBy { it.charOffset }

        // 如果目录条目太少（<2）或太多（>500），认为不是有效目录
        return if (entries.size in 2..500) entries else emptyList()
    }

    /**
     * 根据当前字符偏移量，查找当前所在的章节索引
     */
    fun findCurrentChapter(toc: List<TocEntry>, charOffset: Int): Int {
        if (toc.isEmpty()) return -1
        var result = 0
        for (i in toc.indices) {
            if (toc[i].charOffset <= charOffset) {
                result = i
            } else {
                break
            }
        }
        return result
    }
}
