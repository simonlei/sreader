package com.simon.sreader.core

import java.io.InputStream
import java.nio.charset.Charset

/**
 * 自实现文本编码检测器
 * 检测策略：BOM头检测 → UTF-8 有效性验证 → 回退 GBK
 */
object EncodingDetector {

    /**
     * 检测字节数组的编码格式
     * @param bytes 文件前部分字节（建议至少读取 8KB）
     * @return 检测到的编码名称
     */
    fun detect(bytes: ByteArray): String {
        // 1. BOM 头检测
        detectByBom(bytes)?.let { return it }

        // 2. 尝试 UTF-8 验证
        if (isValidUtf8(bytes)) {
            return "UTF-8"
        }

        // 3. 尝试 GB18030（兼容 GBK 和 GB2312）
        if (isValidGb18030(bytes)) {
            return "GB18030"
        }

        // 4. 尝试 Big5
        if (isValidBig5(bytes)) {
            return "Big5"
        }

        // 5. 回退到 GBK
        return "GBK"
    }

    /**
     * 从输入流检测编码（读取前 8KB 进行检测）
     * @return Pair<编码名称, BOM字节长度>
     */
    fun detectWithBom(inputStream: InputStream): Pair<String, Int> {
        val buffer = ByteArray(8192)
        val bytesRead = inputStream.read(buffer)
        if (bytesRead <= 0) return Pair("UTF-8", 0)
        val bytes = if (bytesRead < buffer.size) buffer.copyOf(bytesRead) else buffer
        val encoding = detect(bytes)
        val bomLength = getBomLength(bytes)
        return Pair(encoding, bomLength)
    }

    /**
     * 从输入流检测编码（读取前 8KB 进行检测）
     * 便捷方法，仅返回编码名称
     */
    fun detect(inputStream: InputStream): String {
        val buffer = ByteArray(8192)
        val bytesRead = inputStream.read(buffer)
        if (bytesRead <= 0) return "UTF-8"
        val bytes = if (bytesRead < buffer.size) buffer.copyOf(bytesRead) else buffer
        return detect(bytes)
    }

    /**
     * BOM (Byte Order Mark) 检测
     */
    private fun detectByBom(bytes: ByteArray): String? {
        if (bytes.size < 2) return null
        // UTF-8 BOM: EF BB BF
        if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
        ) {
            return "UTF-8"
        }
        // UTF-16 LE BOM: FF FE
        if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return "UTF-16LE"
        }
        // UTF-16 BE BOM: FE FF
        if (bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return "UTF-16BE"
        }
        return null
    }

    /**
     * 验证是否为有效的 UTF-8 编码
     * 通过检查多字节序列的合法性来判断
     */
    private fun isValidUtf8(bytes: ByteArray): Boolean {
        var i = 0
        var hasMultiByte = false
        while (i < bytes.size) {
            val b = bytes[i].toInt() and 0xFF
            val expectedLen: Int
            when {
                b <= 0x7F -> { i++; continue } // ASCII
                b in 0xC0..0xDF -> { expectedLen = 2; hasMultiByte = true }
                b in 0xE0..0xEF -> { expectedLen = 3; hasMultiByte = true }
                b in 0xF0..0xF7 -> { expectedLen = 4; hasMultiByte = true }
                else -> return false // 非法的 UTF-8 首字节
            }
            // 检查后续字节是否都是 10xxxxxx 格式
            if (i + expectedLen > bytes.size) break // 末尾不完整不算错
            for (j in 1 until expectedLen) {
                val continuation = bytes[i + j].toInt() and 0xFF
                if (continuation < 0x80 || continuation > 0xBF) return false
            }
            i += expectedLen
        }
        // 纯 ASCII 也算 UTF-8
        return true
    }

    /**
     * 粗略验证是否可能是 GB18030 编码
     */
    private fun isValidGb18030(bytes: ByteArray): Boolean {
        return try {
            val charset = Charset.forName("GB18030")
            val decoded = String(bytes, charset)
            // 如果没有替换字符，说明解码成功
            !decoded.contains('\uFFFD')
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 粗略验证是否可能是 Big5 编码
     */
    private fun isValidBig5(bytes: ByteArray): Boolean {
        return try {
            val charset = Charset.forName("Big5")
            val decoded = String(bytes, charset)
            !decoded.contains('\uFFFD')
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取 BOM 头的字节长度（用于跳过 BOM）
     */
    fun getBomLength(bytes: ByteArray): Int {
        if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
        ) return 3
        if (bytes.size >= 2) {
            if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) return 2
            if (bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) return 2
        }
        return 0
    }
}
