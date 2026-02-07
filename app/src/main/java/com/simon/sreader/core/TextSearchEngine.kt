package com.simon.sreader.core

/**
 * 全文搜索引擎
 */
object TextSearchEngine {

    /**
     * 搜索结果
     */
    data class SearchResult(
        val offset: Int,
        val length: Int
    )

    /**
     * 在文本中搜索关键词（大小写不敏感）
     * @param text 全文文本
     * @param keyword 搜索关键词
     * @return 所有匹配位置列表
     */
    fun search(text: String, keyword: String): List<SearchResult> {
        if (keyword.isEmpty()) return emptyList()

        val results = mutableListOf<SearchResult>()
        val lowerText = text.lowercase()
        val lowerKeyword = keyword.lowercase()
        var startIndex = 0

        while (startIndex < lowerText.length) {
            val found = lowerText.indexOf(lowerKeyword, startIndex)
            if (found == -1) break
            results.add(SearchResult(found, keyword.length))
            startIndex = found + 1
        }

        return results
    }
}
