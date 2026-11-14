package com.example

import com.example.data.card.CardBatch
import com.example.data.card.createMikrotikBatchComment
import com.example.data.card.CardPrintGridSettings
import com.example.data.card.CardTemplate
import com.example.data.card.CardThemePreset
import com.example.data.card.CodeBoxStyle
import com.example.data.card.GeneratedCard
import com.example.data.card.PasswordPattern
import com.example.data.card.PrintPageFormat
import com.example.util.MikroTikCardProvisioner
import com.example.util.QrCodeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardStudioUnitTest {

    @Test
    fun testQrCodeMatrixGeneration() {
        val testUrl = "http://10.50.7.1/login?username=user123&password=pass456"
        val matrix = QrCodeGenerator.encodeToMatrix(testUrl)
        assertNotNull(matrix)
        assertTrue("Matrix size must be >= 21", matrix.size >= 21)
        assertTrue("Matrix width must equal height", matrix.size == matrix[0].size)

        // Corner finder patterns should have black modules at (0,0), (0, matrix.size-1), (matrix.size-1, 0)
        assertTrue(matrix[0][0])
        assertTrue(matrix[0][matrix.size - 1])
        assertTrue(matrix[matrix.size - 1][0])
    }

    @Test
    fun testRouterOsScriptGeneration() {
        val template = CardTemplate(
            networkName = "SamNet",
            priceText = "500 YER",
            quotaText = "2 GB",
            supportPhone = "770446040",
            hotspotLoginUrl = "http://10.50.7.1/login"
        )
        val cards = listOf(
            GeneratedCard(
                serialNumber = "000001",
                username = "771001",
                password = "pin999",
                priceText = "500 YER",
                quotaText = "2 GB",
                validityText = "7 Days",
                networkName = "SamNet",
                supportPhone = "770446040",
                loginUrlWithCredentials = "http://10.50.7.1/login?username=771001&password=pin999",
                batchId = "batch_1"
            ),
            GeneratedCard(
                serialNumber = "000002",
                username = "771002",
                password = "pin888",
                priceText = "500 YER",
                quotaText = "2 GB",
                validityText = "7 Days",
                networkName = "SamNet",
                supportPhone = "770446040",
                loginUrlWithCredentials = "http://10.50.7.1/login?username=771002&password=pin888",
                batchId = "batch_1"
            )
        )
        val batch = CardBatch(
            batchNumber = 101,
            batchTitle = "Test Batch 500",
            templateId = template.id,
            templateSnapshot = template,
            profileName = "2G_1W",
            cardCount = cards.size,
            prefix = "77",
            usernameLength = 6,
            passwordPattern = PasswordPattern.NUMBERS_ONLY,
            cards = cards
        )

        val script = MikroTikCardProvisioner.generateRouterOsScript(batch)
        assertNotNull(script)
        assertTrue("Script must contain /ip hotspot user", script.contains("/ip hotspot user"))
        assertTrue("Script must add user 771001", script.contains("add name=\"771001\" password=\"pin999\""))
        assertTrue("Script must specify profile", script.contains("profile=\"2G_1W\""))
        assertTrue("Script must include technical batch comment", script.contains("comment=\"${batch.technicalMikrotikComment}\""))

        // Test removal script
        val removalScript = MikroTikCardProvisioner.generateRemovalScript(batch)
        assertTrue("Removal script must target technical batch comment", removalScript.contains("/ip hotspot user remove [find comment=\"${batch.technicalMikrotikComment}\"]"))
    }

    @Test
    fun testUserManagerScriptGeneration() {
        val template = CardTemplate()
        val cards = listOf(
            GeneratedCard(
                serialNumber = "000001",
                username = "usrA",
                password = "pwdA",
                priceText = "200",
                quotaText = "1GB",
                validityText = "1Day",
                networkName = "Net",
                supportPhone = "770446040",
                loginUrlWithCredentials = "http://login?username=usrA&password=pwdA",
                batchId = "b1"
            )
        )
        val batch = CardBatch(
            batchNumber = 5,
            batchTitle = "UM Batch",
            templateId = template.id,
            templateSnapshot = template,
            profileName = "prof1",
            cardCount = 1,
            prefix = "",
            usernameLength = 4,
            passwordPattern = PasswordPattern.SAME_AS_USERNAME,
            cards = cards
        )
        val umScript = MikroTikCardProvisioner.generateUserManagerScript(batch)
        assertTrue(umScript.contains("/tool user-manager user"))
        assertTrue(umScript.contains("add username=\"usrA\" password=\"pwdA\" customer=admin"))
    }

    @Test
    fun testPrintPageFormats() {
        assertEquals(24, PrintPageFormat.A4_GRID_24.cardsPerPage)
        assertEquals(3, PrintPageFormat.A4_GRID_24.cols)
        assertEquals(8, PrintPageFormat.A4_GRID_24.rows)

        assertEquals(42, PrintPageFormat.A4_GRID_42.cardsPerPage)
        assertEquals(3, PrintPageFormat.A4_GRID_42.cols)
        assertEquals(14, PrintPageFormat.A4_GRID_42.rows)
        assertEquals(18, PrintPageFormat.A4_GRID_18.cardsPerPage)
        assertEquals(10, PrintPageFormat.A4_GRID_10.cardsPerPage)
        assertEquals(1, PrintPageFormat.THERMAL_ROLL_80MM.cardsPerPage)
    }

    @Test
    fun testCardElementLayoutsUseNormalizedRatios() {
        val template = CardTemplate()
        val layouts = listOf(
            template.priceLayout,
            template.networkLayout,
            template.codeBoxLayout,
            template.qrCodeLayout,
            template.metaLayout,
            template.supportPhoneLayout,
            template.creationDateLayout,
            template.serialLayout
        )

        layouts.forEach { layout ->
            assertTrue(layout.xRatio in 0f..1f)
            assertTrue(layout.yRatio in 0f..1f)
            assertTrue(layout.widthRatio in 0f..1f)
            assertTrue(layout.heightRatio in 0f..1f)
        }

        val codeLayout = template.codeBoxLayout
        assertEquals(0.29f, codeLayout.leftRatio, 0.001f)
        assertEquals(0.39f, codeLayout.topRatio, 0.001f)
        assertEquals(0.91f, codeLayout.rightRatio, 0.001f)
        assertEquals(0.69f, codeLayout.bottomRatio, 0.001f)
    }

    @Test
    fun testRouterBatchCommentIsAsciiAndProfileSafe() {
        val comment = createMikrotikBatchComment("بروفايل 200", 1_794_456_000_000L, 15L)

        assertTrue(comment.all { it.code in 0x20..0x7E })
        assertTrue(comment.startsWith("card"))
        assertTrue(comment.endsWith("_B15"))
        assertTrue(comment.contains("200"))
    }

    @Test
    fun testPrintGridCellDimensionsAndPageBounds() {
        val defaults = CardPrintGridSettings().normalized()
        assertEquals(3, defaults.columns)
        assertEquals(8, defaults.rows)
        assertEquals(2.5f, defaults.horizontalGapMm, 0.001f)
        assertEquals(2.5f, defaults.verticalGapMm, 0.001f)
        assertEquals(6.5f, defaults.horizontalMarginMm, 0.001f)
        assertEquals(6f, defaults.verticalMarginMm, 0.001f)

        val miniGrid = defaults.copy(rows = 14)
        assertEquals(64f, miniGrid.availableCellWidthMm(), 0.001f)
        assertEquals((297f - 12f - 13 * 2.5f) / 14f, miniGrid.availableCellHeightMm(), 0.001f)

        val grid = CardPrintGridSettings(
            isCustom = true,
            columns = 3,
            rows = 8,
            horizontalGapMm = 2f,
            verticalGapMm = 2f,
            horizontalMarginMm = 4f,
            verticalMarginMm = 8f
        )
        assertEquals(66f, grid.availableCellWidthMm(), 0.001f)
        assertEquals(33.375f, grid.availableCellHeightMm(), 0.001f)

        val bounded = CardPrintGridSettings(
            columns = 5,
            rows = 16,
            horizontalGapMm = 100f,
            verticalGapMm = 100f,
            horizontalMarginMm = 40f,
            verticalMarginMm = 40f
        ).normalized()
        assertTrue(2f * bounded.horizontalMarginMm + (bounded.columns - 1) * bounded.horizontalGapMm <= 210f)
        assertTrue(2f * bounded.verticalMarginMm + (bounded.rows - 1) * bounded.verticalGapMm <= 297f)
    }

    @Test
    fun testCardTemplatePresetsAndStyles() {
        val preset = CardThemePreset.CYBER_NEON
        assertTrue(preset.displayNameAr.contains("سايبر نيون"))

        val styles = CodeBoxStyle.values()
        assertTrue(styles.contains(CodeBoxStyle.PILL_BADGE))
        assertTrue(styles.contains(CodeBoxStyle.MODERN_BOX))
        assertTrue(styles.contains(CodeBoxStyle.SPLIT_DIGITS))
        assertTrue(styles.contains(CodeBoxStyle.SCRATCH_FOIL))
        assertTrue(styles.contains(CodeBoxStyle.TICKET_DASHED))
    }
}
