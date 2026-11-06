package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ShadowPdfDocument
import com.example.data.card.CardBatch
import com.example.data.card.CardStudioRepository
import com.example.data.card.CardTemplate
import com.example.data.card.CardThemePreset
import com.example.data.card.GeneratedCard
import com.example.data.card.PasswordPattern
import com.example.data.card.PrintPageFormat
import com.example.util.CardPdfDocumentGenerator
import com.example.util.MikroTikCardProvisioner
import com.example.util.QrCodeGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [ShadowPdfDocument::class])
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
    fun testQrBitmapGeneration() {
        val testUrl = "http://wifi.net/login?username=admin&password=777"
        val bitmap = QrCodeGenerator.generateQrBitmap(testUrl, sizePx = 128)
        assertNotNull(bitmap)
        assertEquals(128, bitmap.width)
        assertEquals(128, bitmap.height)
    }

    @Test
    fun testRouterOsScriptGeneration() {
        val template = CardTemplate(
            networkName = "SamNet",
            priceText = "500 YER",
            quotaText = "2 GB",
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
    fun testCardBatchGenerationInRepository() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = CardStudioRepository(context)

        val template = repo.templates.first().firstOrNull() ?: CardTemplate()
        val generatedBatch = repo.generateBatch(
            title = "دفعة تجريبية فئة 1000",
            template = template,
            profileName = "VIP_5G",
            count = 15,
            prefix = "99",
            usernameLength = 7,
            passwordPattern = PasswordPattern.SAME_AS_USERNAME
        )

        assertEquals(15, generatedBatch.cardCount)
        assertEquals(15, generatedBatch.cards.size)

        // All usernames must start with prefix "99"
        assertTrue(generatedBatch.cards.all { it.username.startsWith("99") })

        // Check uniqueness of generated usernames
        val uniqueUsernames = generatedBatch.cards.map { it.username }.toSet()
        assertEquals(15, uniqueUsernames.size)

        // Check password matches username when pattern is SAME_AS_USERNAME
        assertTrue(generatedBatch.cards.all { it.password == it.username })

        // Check login URL contains the credentials
        val firstCard = generatedBatch.cards.first()
        assertTrue(firstCard.loginUrlWithCredentials.contains("username=${firstCard.username}"))
        assertTrue(firstCard.loginUrlWithCredentials.contains("password=${firstCard.password}"))
    }

    @Test
    fun testCardPdfGenerationA4AndThermal() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val template = CardTemplate(
            networkName = "شبكة سام",
            priceText = "500 ر.ي",
            quotaText = "2 جيجا"
        )
        val cards = (1..6).map { i ->
            GeneratedCard(
                serialNumber = String.format(java.util.Locale.US, "%06d", i),
                username = "77000$i",
                password = "77000$i",
                priceText = "500 ر.ي",
                quotaText = "2 جيجا",
                validityText = "7 أيام",
                networkName = "شبكة سام",
                loginUrlWithCredentials = "http://10.50.7.1/login?username=77000$i&password=77000$i",
                batchId = "b1"
            )
        }
        val batch = CardBatch(
            batchNumber = 77,
            batchTitle = "دفعة طباعة A4",
            templateId = template.id,
            templateSnapshot = template,
            profileName = "default",
            cardCount = cards.size,
            prefix = "77",
            usernameLength = 6,
            passwordPattern = PasswordPattern.SAME_AS_USERNAME,
            cards = cards
        )

        // Test A4 Grid PDF Generation
        val a4Pdf = CardPdfDocumentGenerator.generateBatchPdf(context, batch, PrintPageFormat.A4_GRID_10)
        assertNotNull(a4Pdf)
        assertTrue("A4 PDF file must exist", a4Pdf.exists())
        assertTrue("A4 PDF file must not be empty", a4Pdf.length() > 0)

        // Test Thermal Roll PDF Generation
        val thermalPdf = CardPdfDocumentGenerator.generateBatchPdf(context, batch, PrintPageFormat.THERMAL_ROLL_80MM)
        assertNotNull(thermalPdf)
        assertTrue("Thermal PDF file must exist", thermalPdf.exists())
        assertTrue("Thermal PDF file must not be empty", thermalPdf.length() > 0)
    }
}
