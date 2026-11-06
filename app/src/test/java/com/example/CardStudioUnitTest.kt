package com.example

import com.example.data.card.CardBatch
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
        assertTrue("Script must include batch comment", script.contains("comment=\"Batch_101_"))

        // Test removal script
        val removalScript = MikroTikCardProvisioner.generateRemovalScript(batch)
        assertTrue("Removal script must target batch comment", removalScript.contains("/ip hotspot user remove [find comment=\"Batch_101_"))
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

        assertEquals(18, PrintPageFormat.A4_GRID_18.cardsPerPage)
        assertEquals(10, PrintPageFormat.A4_GRID_10.cardsPerPage)
        assertEquals(1, PrintPageFormat.THERMAL_ROLL_80MM.cardsPerPage)
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
