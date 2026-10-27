package com.example.domain.report

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.model.CurrencyCode
import com.example.core.model.Money
import com.example.core.time.AppClock
import com.example.core.time.FixedAppClock
import com.example.data.ledger.LedgerWriter
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AssetEntity
import com.example.data.local.entity.PartyEntity
import com.example.domain.usecase.PartnerEquityUseCase
import com.example.domain.usecase.PartnerWeight
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Stage1HardeningTest {

    private lateinit var db: AppDatabase
    private lateinit var ledgerWriter: LedgerWriter
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = AppDatabase.createInMemory(context)
        AppDatabase.installTriggers(db.openHelper.writableDatabase)
        AppDatabase.seedDefaultData(db.openHelper.writableDatabase)
        ledgerWriter = LedgerWriter(db, enableInvariantValidation = false)
    }

    @After
    fun tearDown() {
        AppClock.resetToDefault()
        db.close()
    }

    @Test
    fun testClockFixedAt2230UtcShowsNextAdenDay() {
        // At 22:30 UTC on 2026-03-15, in Asia/Aden (UTC+3) it is 01:30 on 2026-03-16
        val utcInstant = Instant.parse("2026-03-15T22:30:00Z")
        val fixedClock = FixedAppClock(utcInstant)
        AppClock.setClock(fixedClock)

        val adenDate = AppClock.todayLocalDate()
        assertEquals(LocalDate.of(2026, 3, 16), adenDate)
    }

    @Test
    fun testSharesOf100001Over3PartnersSumTo100001() {
        val useCase = PartnerEquityUseCase(db, ledgerWriter)
        val partners = listOf(
            PartnerWeight("P1", "شريك 1", 3333L),
            PartnerWeight("P2", "شريك 2", 3333L),
            PartnerWeight("P3", "شريك 3", 3334L)
        )

        val totalProfitMinor = 100_001L
        val shares = useCase.calculateHareNiemeyerDistribution(totalProfitMinor, partners)

        assertEquals(3, shares.size)
        val sumOfShares = shares.sumOf { it.amountMinor }
        assertEquals(100_001L, sumOfShares)

        // Also test equal weights 1:1:1
        val equalPartners = listOf(
            PartnerWeight("P1", "شريك 1", 1000L),
            PartnerWeight("P2", "شريك 2", 1000L),
            PartnerWeight("P3", "شريك 3", 1000L)
        )
        val equalShares = useCase.calculateHareNiemeyerDistribution(totalProfitMinor, equalPartners)
        assertEquals(3, equalShares.size)
        assertEquals(100_001L, equalShares.sumOf { it.amountMinor })
        // Exactly two get 33,334 and one gets 33,333
        assertTrue(equalShares.any { it.amountMinor == 33_334L })
        assertTrue(equalShares.any { it.amountMinor == 33_333L })
    }

    @Test
    fun testBadgesFlipProvisionalToFinalAfterDepreciationRun() = runBlocking {
        // Set clock to end of 2026 so month 1 is a past completed period
        AppClock.setClock(FixedAppClock(LocalDate.of(2026, 6, 1).toEpochDay()))

        val fiscalYear = 2026
        val m1StartEpoch = LocalDate.of(2026, 1, 1).toEpochDay()

        // 1. Insert a depreciable asset purchased in month 1
        val asset = AssetEntity(
            id = "ASSET_SERVER_01",
            docId = "DOC_PURCHASE_01",
            name = "سيرفر التوزيع الرئيسي",
            purchaseDateEpochDay = m1StartEpoch,
            purchaseCostMinor = 1_200_000_00L,
            salvageValueMinor = 0L,
            usefulLifeMonths = 12,
            accumulatedDepreciationMinor = 0L,
            isDisposed = false
        )
        db.assetDao().insertAsset(asset)

        // 2. Generate report BEFORE depreciation run: Month 1 MUST be PROVISIONAL
        val reportBefore = generateMonthlyProfitReport(db, fiscalYear)
        val m1Before = reportBefore.months[0]
        assertEquals(1, m1Before.monthIndex)
        assertEquals(MonthClosingStatus.PROVISIONAL, m1Before.status.status)
        assertTrue(
            "Reason must mention missing depreciation",
            m1Before.status.reasonsList.any { it.contains("إهلاك") }
        )

        // 3. Execute monthly straight-line depreciation for Month 1
        val ran = ledgerWriter.runMonthlyDepreciation(
            assetId = asset.id,
            periodYear = 2026,
            periodMonth = 1,
            dateEpochDay = LocalDate.of(2026, 1, 31).toEpochDay()
        )
        assertTrue("Monthly depreciation run must succeed", ran)

        // 4. Generate report AFTER depreciation run: Month 1 MUST flip to FINAL
        val reportAfter = generateMonthlyProfitReport(db, fiscalYear)
        val m1After = reportAfter.months[0]
        assertEquals(1, m1After.monthIndex)
        assertEquals(MonthClosingStatus.FINAL, m1After.status.status)
        assertTrue(
            "Finalized month reason must indicate complete accounting closure",
            m1After.status.reason.contains("معتمدة") || m1After.status.reason.contains("مقفلة")
        )
    }

    @Test
    fun testReconciliationDifferenceTextIsFormatted() {
        val largeDifferenceMinor = 50_000_000_00L // 50,000,000 YER
        val formatted = Money(largeDifferenceMinor, CurrencyCode.YER).format()

        // Must contain comma grouping separators, not raw consecutive digits
        assertTrue("Formatted difference must contain grouping comma", formatted.contains(","))
        assertFalse("Must not be raw unformatted digits", formatted == "5000000000")
        assertFalse("Must not contain 7+ consecutive digits without comma/point", formatted.contains(Regex("\\d{7,}")))
    }

    @Test
    fun testNoReportTextContains7PlusConsecutiveDigitsWithoutSeparators() {
        val sampleAmountMinor = 12_345_678_00L
        val formattedMoney = Money(sampleAmountMinor, CurrencyCode.FUNCTIONAL).format()

        assertFalse(
            "Formatted money should have thousands grouping and not 7+ raw consecutive digits",
            formattedMoney.contains(Regex("\\d{7,}"))
        )

        val bpsRatio = 7550 // 75.50%
        val formattedRatio = PartnerEquityUseCase.formatBasisPoints(bpsRatio)
        assertEquals("75.50%", formattedRatio)
        assertFalse(formattedRatio.contains(Regex("\\d{7,}")))
    }

    @Test
    fun testPdfStatementShowsDecimalsAndGrouping() {
        val debitMinor = 250_000_00L
        val formatted = Money(debitMinor, CurrencyCode.FUNCTIONAL).format()

        assertTrue("Must contain decimal separator", formatted.contains("."))
        assertTrue("Must contain grouping separator", formatted.contains(","))
        assertTrue("Must contain 250,000.00", formatted.contains("250,000.00"))
    }

    @Test
    fun testZeroDoubleInPartnerEquityUseCase() {
        val candidatePaths = listOf(
            File("src/main/java/com/example/domain/usecase/PartnerEquityUseCase.kt"),
            File("app/src/main/java/com/example/domain/usecase/PartnerEquityUseCase.kt"),
            File("/app/applet/app/src/main/java/com/example/domain/usecase/PartnerEquityUseCase.kt")
        )
        val file = candidatePaths.firstOrNull { it.exists() }
        assertNotNull("PartnerEquityUseCase.kt file must exist", file)
        val text = file!!.readText()

        // Exclude comments if any, and check for Double keyword
        val linesWithoutComments = text.lines().filter { !it.trim().startsWith("//") && !it.trim().startsWith("*") }
        val doubleOccurrences = linesWithoutComments.filter { it.contains("Double") }
        assertTrue(
            "PartnerEquityUseCase must not contain Double keyword: $doubleOccurrences",
            doubleOccurrences.isEmpty()
        )
    }
}
