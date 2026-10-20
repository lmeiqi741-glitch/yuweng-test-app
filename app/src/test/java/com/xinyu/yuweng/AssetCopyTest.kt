package com.xinyu.yuweng

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.io.File

/**
 * 验证 assets 中的 spec 文件与源文件逐字节一致。
 * 对应任务一第 6 条：system prompt 原文注入的数据源必须与源文件逐字节一致。
 */
class AssetCopyTest {

    private val specsDir = File("app/src/main/assets/specs")
    private val sourceDir = File("/var/minis/attachments/uploads")

    @Test
    fun systemPromptIsIdenticalToSource() {
        val assetFile = File(specsDir, "01-屿翁system-prompt.txt")
        val sourceFile = File(sourceDir, "__system_prompt.txt")

        assert(assetFile.exists()) { "Asset file not found: ${assetFile.path}" }
        assert(sourceFile.exists()) { "Source file not found: ${sourceFile.path}" }

        val assetBytes = assetFile.readBytes()
        val sourceBytes = sourceFile.readBytes()

        assertArrayEquals(
            "01-system-prompt.txt must be byte-identical to source",
            sourceBytes,
            assetBytes
        )
    }

    @Test
    fun stateMemorySpecIsIdenticalToSource() {
        val assetFile = File(specsDir, "02-屿翁状态记忆库设定.txt")
        val sourceFile = File(sourceDir, "_________.txt")

        assert(assetFile.exists()) { "Asset file not found: ${assetFile.path}" }
        assert(sourceFile.exists()) { "Source file not found: ${sourceFile.path}" }

        val assetBytes = assetFile.readBytes()
        val sourceBytes = sourceFile.readBytes()

        assertArrayEquals(
            "02-state-memory.txt must be byte-identical to source",
            sourceBytes,
            assetBytes
        )
    }

    @Test
    fun uiUxSpecIsIdenticalToSource() {
        val assetFile = File(specsDir, "03-心屿UI-UX设计.txt")
        val sourceFile = File(sourceDir, "UI_UX__.txt")

        assert(assetFile.exists()) { "Asset file not found: ${assetFile.path}" }
        assert(sourceFile.exists()) { "Source file not found: ${sourceFile.path}" }

        val assetBytes = assetFile.readBytes()
        val sourceBytes = sourceFile.readBytes()

        assertArrayEquals(
            "03-ui-ux.txt must be byte-identical to source",
            sourceBytes,
            assetBytes
        )
    }
}
