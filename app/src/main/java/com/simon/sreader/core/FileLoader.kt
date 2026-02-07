package com.simon.sreader.core

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset

/**
 * 文件加载器：通过 SAF URI 读取文本文件内容
 */
object FileLoader {

    /**
     * 加载文件内容（无进度回调版本，保持向后兼容）
     * @return Pair<编码格式, 文本内容>
     */
    fun loadFile(context: Context, uri: Uri): Pair<String, String> {
        return loadFile(context, uri, onProgress = null)
    }

    /**
     * 加载文件内容（带进度回调版本）
     *
     * 两阶段加载流程：
     * 1. 先读取前 8KB 进行编码检测
     * 2. 使用检测到的编码，通过 BufferedReader 流式读取全文
     *
     * @param onProgress 进度回调 (阶段描述, 进度百分比 0f~1f)，传 null 则不报告进度
     * @return Pair<编码格式, 文本内容>
     */
    fun loadFile(
        context: Context,
        uri: Uri,
        onProgress: ((phase: String, progress: Float) -> Unit)?
    ): Pair<String, String> {
        val contentResolver = context.contentResolver

        // 获取文件大小，用于计算进度
        val fileSize = getFileSize(context, uri)

        onProgress?.invoke("正在检测编码...", 0f)

        // 第一阶段：读取前 8KB 进行编码检测
        val (encoding, bomLength) = contentResolver.openInputStream(uri)?.use { inputStream ->
            val buffered = BufferedInputStream(inputStream)
            EncodingDetector.detectWithBom(buffered)
        } ?: throw IllegalStateException("无法打开文件: $uri")

        val charset = try {
            Charset.forName(encoding)
        } catch (e: Exception) {
            Charsets.UTF_8
        }

        onProgress?.invoke("正在读取文件...", 0.05f)

        // 第二阶段：使用检测到的编码，流式读取全文
        val text = contentResolver.openInputStream(uri)?.use { inputStream ->
            // 跳过 BOM 头
            if (bomLength > 0) {
                var skipped = 0L
                while (skipped < bomLength) {
                    val s = inputStream.skip(bomLength.toLong() - skipped)
                    if (s <= 0) break
                    skipped += s
                }
            }

            val reader = BufferedReader(InputStreamReader(inputStream, charset), 64 * 1024)
            if (fileSize > 0 && onProgress != null) {
                // 有文件大小信息时，报告读取进度
                val sb = StringBuilder(if (fileSize > Int.MAX_VALUE) Int.MAX_VALUE else fileSize.toInt())
                val buffer = CharArray(32 * 1024)
                var totalCharsRead = 0L
                var lastReportedPercent = 0
                var charsRead: Int
                while (reader.read(buffer).also { charsRead = it } != -1) {
                    sb.append(buffer, 0, charsRead)
                    totalCharsRead += charsRead
                    // 粗略估算读取进度（字符数 vs 文件字节数，非精确但实用）
                    val estimatedProgress = (totalCharsRead.toFloat() / (fileSize / averageCharBytes(charset))).coerceIn(0f, 0.95f)
                    val currentPercent = (estimatedProgress * 100).toInt()
                    if (currentPercent > lastReportedPercent) {
                        lastReportedPercent = currentPercent
                        onProgress.invoke("正在读取文件...", estimatedProgress)
                    }
                }
                sb.toString()
            } else {
                // 无文件大小或无进度回调时，直接读取
                reader.readText()
            }
        } ?: throw IllegalStateException("无法打开文件: $uri")

        onProgress?.invoke("文件读取完成", 1f)
        return Pair(encoding, text)
    }

    /**
     * 根据字符集估算平均每个字符占用的字节数
     */
    private fun averageCharBytes(charset: Charset): Float {
        return when (charset.name().uppercase()) {
            "UTF-8" -> 1.5f  // ASCII为主时接近1，中文为主时接近3
            "UTF-16LE", "UTF-16BE", "UTF-16" -> 2f
            "US-ASCII" -> 1f
            else -> 2f  // GBK、GB18030、Big5 等双字节编码
        }
    }

    /**
     * 获取文件大小（字节数）
     * @return 文件大小，无法获取时返回 -1
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex >= 0) cursor.getLong(sizeIndex) else -1L
                } else -1L
            } ?: -1L
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * 获取文件显示名称
     */
    fun getFileName(context: Context, uri: Uri): String {
        var name = "未知文件"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    name = cursor.getString(nameIndex) ?: name
                }
            }
        }
        return name
    }

    /**
     * 检查 URI 是否仍可访问
     */
    fun isUriAccessible(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
