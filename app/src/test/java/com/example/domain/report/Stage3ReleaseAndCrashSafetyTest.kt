package com.example.domain.report

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.model.CurrencyCode
import com.example.core.model.ExchangeRate
import com.example.data.auth.AuthManager
import com.example.data.auth.GoogleAuthManager
import com.example.data.ledger.LedgerWriter
import com.example.data.ledger.PaymentSource
import com.example.data.ledger.PaymentVoucherType
import com.example.data.local.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Stage3ReleaseAndCrashSafetyTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var writer: LedgerWriter

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = AppDatabase.createInMemory(context)
        writer = LedgerWriter(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testNoDoubleBangAssertionsRemainInProductionCode() {
        val mainJavaDir = File("src/main/java")
        val candidateDir = if (mainJavaDir.exists()) mainJavaDir else File("app/src/main/java")
        assertTrue("Main source directory must exist", candidateDir.exists())

        val violations = mutableListOf<String>()
        candidateDir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val lines = file.readLines()
            lines.forEachIndexed { index, line ->
                // Check for Kotlin's !! operator (ignoring comments)
                val cleanLine = line.substringBefore("//")
                if (cleanLine.contains("!!")) {
                    violations.add("${file.name}:${index + 1}: $line")
                }
            }
        }

        assertTrue(
            "Found double-bang (!!) assertions in production code violating crash-safety rule 5:\n" +
                    violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun testProGuardRulesFileIsValidAndContainsCriticalKeepRules() {
        val rulesFile = File("proguard-rules.pro")
        val targetFile = if (rulesFile.exists()) rulesFile else File("app/proguard-rules.pro")
        assertTrue("ProGuard rules file must exist", targetFile.exists())

        val content = targetFile.readText()
        assertTrue("Must keep Room database classes", content.contains("androidx.room.RoomDatabase"))
        assertTrue("Must keep Room Dao interfaces", content.contains("@androidx.room.Dao"))
        assertTrue("Must keep Room Entity classes", content.contains("@androidx.room.Entity"))
        assertTrue("Must keep Domain models", content.contains("com.example.domain.model.**"))
        assertTrue("Must keep Moshi serialization adapters", content.contains("com.squareup.moshi.*"))
        assertTrue("Must keep Firebase classes", content.contains("com.google.firebase.**"))
        assertTrue("Must keep Credential Manager classes", content.contains("androidx.credentials.**"))
        assertTrue("Must keep Coroutines dispatchers", content.contains("kotlinx.coroutines.internal.MainDispatcherFactory"))
        assertTrue("Must keep Compose runtime", content.contains("androidx.compose.**"))
        assertTrue("Must preserve line numbers and source file attributes", content.contains("-keepattributes SourceFile,LineNumberTable"))
    }

    @Test
    fun testReleaseBuildConfigurationHandlesMissingKeystoresGracefully() {
        val gradleFile = File("build.gradle.kts")
        val targetGradleFile = if (gradleFile.exists()) gradleFile else File("app/build.gradle.kts")
        assertTrue("build.gradle.kts must exist", targetGradleFile.exists())

        val content = targetGradleFile.readText()
        assertTrue("Must check keystoreFile.exists() before setting storeFile", content.contains("keystoreFile.exists()"))
        assertTrue("Must have safe fallback to debug keystore for development / CI builds", content.contains("debugKeystore.exists()"))
        assertTrue("Must have isMinifyEnabled = true for release builds", content.contains("isMinifyEnabled = true"))
    }

    @Test
    fun testPaymentVoucherSafeNullHandlingWithoutUncheckedNullPointer() = runBlocking {
        try {
            // Invoking with null treasury when paymentSource is cash/bank
            writer.postPaymentVoucher(
                recipientPartyId = "NON_EXISTENT",
                treasuryId = null,
                fiscalYear = 2026,
                dateEpochDay = 20500L,
                amountOrigMinor = 1000L,
                currency = CurrencyCode.YER,
                exchangeRate = ExchangeRate.parity(CurrencyCode.YER),
                paymentType = PaymentVoucherType.OPERATING_EXPENSE,
                paymentSource = PaymentSource.TREASURY,
                payingPartnerPartyId = null
            )
            fail("Expected IllegalArgumentException when treasury is null for cash drawer payment")
        } catch (e: IllegalArgumentException) {
            assertTrue("Should contain descriptive message about treasury", e.message?.contains("treasury", ignoreCase = true) == true)
        }

        try {
            // Invoking with null partner when paymentSource is PARTNER_PERSONAL
            writer.postPaymentVoucher(
                recipientPartyId = "NON_EXISTENT",
                treasuryId = null,
                fiscalYear = 2026,
                dateEpochDay = 20500L,
                amountOrigMinor = 1000L,
                currency = CurrencyCode.YER,
                exchangeRate = ExchangeRate.parity(CurrencyCode.YER),
                paymentType = PaymentVoucherType.OPERATING_EXPENSE,
                paymentSource = PaymentSource.PARTNER_PERSONAL,
                payingPartnerPartyId = null
            )
            fail("Expected IllegalArgumentException when payingPartnerPartyId is null for partner personal payment")
        } catch (e: IllegalArgumentException) {
            assertTrue("Should contain descriptive message about paying partner", e.message?.contains("partner", ignoreCase = true) == true)
        }
    }

    @Test
    fun testAuthManagerSafeSessionRestoreWithoutNullPointer() {
        val authManager = AuthManager(context)
        assertNotNull("AuthManager must initialize without throwing", authManager)
        // In clean test context with no persisted session, currentUser is null and that is safe
        val session = authManager.currentUser.value
        assertFalse("Clean session should not be signed in", session?.isSignedIn ?: false)
    }

    @Test
    fun testGoogleAuthManagerSafeInitializationWithoutCrash() {
        val googleAuthManager = GoogleAuthManager(context)
        assertNotNull("GoogleAuthManager must initialize safely in headless JVM", googleAuthManager)
        val profile = googleAuthManager.userProfile.value
        val isAuthed = profile?.isAuthenticated ?: false
        assertFalse("Clean state should not be authenticated", isAuthed)
    }
}
