package com.example.domain.report

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.ledger.AccountConstants
import com.example.core.ledger.DocumentType
import com.example.core.ledger.PartnerDividendSpec
import com.example.core.model.CurrencyCode
import com.example.core.model.ExchangeRate
import com.example.core.model.UuidUtils
import com.example.data.ledger.LedgerWriter
import com.example.data.ledger.PaymentSource
import com.example.data.ledger.PaymentVoucherType
import com.example.data.local.AppDatabase
import com.example.data.local.entity.JournalEntryEntity
import com.example.data.local.entity.JournalLineEntity
import com.example.data.local.entity.PartyEntity
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
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PartnerStatementAndEquityTest {

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
        db.close()
    }

    @Test
    fun testPartnerStatementMultiCurrencyContributionsWithdrawalsAndInKindAsset() = runBlocking {
        val fiscalYear = 2026
        val partner = PartyEntity(
            id = "PARTNER_ENG_SAM",
            name = "المهندس سام (شريك مؤسس)",
            isPartner = true,
            equityPercentageBasisPoints = 6000 // 60%
        )
        val vendor = PartyEntity(
            id = "VEND_MIKROTIK",
            name = "وكيل مايكروتك دبي",
            isVendor = true
        )
        db.partyDao().insertParty(partner)
        db.partyDao().insertParty(vendor)

        db.treasuryDao().setAllowNegative("TR_MAIN_YER", true)
        db.treasuryDao().setAllowNegative("TR_USD_VAULT", true)

        val jan1 = LocalDate.of(2026, 1, 1).toEpochDay()

        // 1. Cash Contribution in YER to Capital (3101): 1,000,000 YER (100,000,000 minor)
        ledgerWriter.postCapitalReceipt(
            targetAccountCode = AccountConstants.CAPITAL,
            partnerPartyId = partner.id,
            treasuryId = "TR_MAIN_YER",
            fiscalYear = fiscalYear,
            dateEpochDay = jan1,
            amountOrigMinor = 100_000_000L,
            currency = CurrencyCode.YER,
            exchangeRate = ExchangeRate.parity(CurrencyCode.YER),
            notes = "دفعة رأس مال تأسيسي أولى نقدية"
        )

        // 2. In-Kind Asset Contribution to Capital (3101): Router CCR2004 worth 400,000 YER (40,000,000 minor)
        val jan5 = LocalDate.of(2026, 1, 5).toEpochDay()
        ledgerWriter.postInKindCapitalContribution(
            partnerPartyId = partner.id,
            assetName = "راوتر مايكروتك CCR2004 رئيسي",
            fiscalYear = fiscalYear,
            dateEpochDay = jan5,
            amountOrigMinor = 40_000_000L,
            currency = CurrencyCode.YER,
            exchangeRate = ExchangeRate.parity(CurrencyCode.YER),
            notes = "مساهمة عينية براوتر سيرفرات"
        )

        // 3. Multi-Currency Contribution in USD to Partner Current (3201): $1,000 @ 530 YER/USD = 530,000 YER
        val jan10 = LocalDate.of(2026, 1, 10).toEpochDay()
        val usdRate = ExchangeRate(CurrencyCode.USD, CurrencyCode.YER, 530_000_000L) // 530 YER/USD
        ledgerWriter.postCapitalReceipt(
            targetAccountCode = AccountConstants.PARTNER_CURRENT,
            partnerPartyId = partner.id,
            treasuryId = "TR_USD_VAULT",
            fiscalYear = fiscalYear,
            dateEpochDay = jan10,
            amountOrigMinor = 100_000L, // $1,000.00
            currency = CurrencyCode.USD,
            exchangeRate = usdRate,
            notes = "تمويل طارئ بالدولار للحساب الجاري"
        )

        // 4. Partner Paid On Behalf (Out-of-Pocket): Partner paid vendor $200 (106,000 YER) directly
        val jan15 = LocalDate.of(2026, 1, 15).toEpochDay()
        ledgerWriter.postPaymentVoucher(
            recipientPartyId = vendor.id,
            amountOrigMinor = 20_000L, // $200.00
            currency = CurrencyCode.USD,
            exchangeRate = usdRate,
            fiscalYear = fiscalYear,
            dateEpochDay = jan15,
            paymentType = PaymentVoucherType.OPERATING_EXPENSE,
            paymentSource = PaymentSource.PARTNER_PERSONAL,
            payingPartnerPartyId = partner.id,
            notes = "سداد مستحقات مايكروتك من الحساب الشخصي للشريك"
        )

        // 5. Partner Drawings / Withdrawal from Cash Box: 50,000 YER (5,000,000 minor)
        val jan20 = LocalDate.of(2026, 1, 20).toEpochDay()
        ledgerWriter.postPaymentVoucher(
            recipientPartyId = partner.id,
            amountOrigMinor = 5_000_000L,
            currency = CurrencyCode.YER,
            exchangeRate = ExchangeRate.parity(CurrencyCode.YER),
            fiscalYear = fiscalYear,
            dateEpochDay = jan20,
            paymentSource = PaymentSource.TREASURY,
            treasuryId = "TR_MAIN_YER",
            paymentType = PaymentVoucherType.PARTNER_DRAWINGS,
            notes = "مسحوبات شخصية للمصاريف العائلية"
        )

        // Generate Partner Statement Report
        val fromEpoch = LocalDate.of(2026, 1, 1).toEpochDay()
        val toEpoch = LocalDate.of(2026, 1, 31).toEpochDay()
        val statement = generatePartnerStatementReport(
            db = db,
            partnerPartyId = partner.id,
            fromEpochDay = fromEpoch,
            toEpochDay = toEpoch
        )

        // Verifications
        assertEquals(partner.id, statement.partnerPartyId)
        assertEquals(partner.name, statement.partnerName)
        assertEquals(0L, statement.capitalOpeningBalanceMinor)
        assertEquals(0L, statement.currentOpeningBalanceMinor)

        // Closing Capital: 100,000,000 (Cash) + 40,000,000 (In-Kind) = 140,000,000 YER
        assertEquals(140_000_000L, statement.capitalClosingBalanceMinor)

        // Closing Current: 53,000,000 ($1000 @ 530) + 10,600,000 (Paid on behalf $200 @ 530) - 5,000,000 (Withdrawal) = 58,600,000 YER
        assertEquals(58_600_000L, statement.currentClosingBalanceMinor)

        // Movement lines count: 5 motions
        assertEquals(5, statement.lines.size)

        // Verify In-Kind line shows Asset Name
        val inKindLine = statement.lines.firstOrNull { it.motionType == PartnerMotionType.IN_KIND_CONTRIBUTION }
        assertNotNull("In-kind contribution line must exist", inKindLine)
        assertTrue(
            "In-kind line description must contain asset name",
            inKindLine!!.description.contains("CCR2004")
        )

        // Verify Motion Types
        assertEquals(PartnerMotionType.CASH_CONTRIBUTION, statement.lines[0].motionType)
        assertEquals(PartnerMotionType.IN_KIND_CONTRIBUTION, statement.lines[1].motionType)
        assertEquals(PartnerMotionType.CASH_CONTRIBUTION, statement.lines[2].motionType)
        assertEquals(PartnerMotionType.PAID_ON_BEHALF, statement.lines[3].motionType)
        assertEquals(PartnerMotionType.WITHDRAWAL, statement.lines[4].motionType)

        // Verify Running Balances monotonicity and tracking
        assertEquals(100_000_000L, statement.lines[0].capitalRunningBalanceMinor)
        assertEquals(140_000_000L, statement.lines[1].capitalRunningBalanceMinor)
        assertEquals(53_000_000L, statement.lines[2].currentRunningBalanceMinor)
        assertEquals(63_600_000L, statement.lines[3].currentRunningBalanceMinor)
        assertEquals(58_600_000L, statement.lines[4].currentRunningBalanceMinor)

        // Reconciliation
        assertTrue("Partner Statement must be reconciled", statement.reconciliation.reconciled)
    }

    @Test
    fun testStatementOfChangesInEquityAndReconciliation() = runBlocking {
        val fiscalYear = 2026
        val partner1 = PartyEntity(
            id = "PARTNER_1",
            name = "الشريك الأول (أصيل)",
            isPartner = true,
            equityPercentageBasisPoints = 6000
        )
        val partner2 = PartyEntity(
            id = "PARTNER_2",
            name = "الشريك الثاني (بسام)",
            isPartner = true,
            equityPercentageBasisPoints = 4000
        )
        db.partyDao().insertParty(partner1)
        db.partyDao().insertParty(partner2)
        db.treasuryDao().setAllowNegative("TR_MAIN_YER", true)

        val jan1 = LocalDate.of(2026, 1, 1).toEpochDay()
        val rate = ExchangeRate.parity(CurrencyCode.YER)

        // Capital contributions: 60M and 40M
        ledgerWriter.postCapitalReceipt(
            targetAccountCode = AccountConstants.CAPITAL,
            partnerPartyId = partner1.id,
            treasuryId = "TR_MAIN_YER",
            fiscalYear = fiscalYear,
            dateEpochDay = jan1,
            amountOrigMinor = 60_000_000L,
            currency = CurrencyCode.YER,
            exchangeRate = rate
        )
        ledgerWriter.postCapitalReceipt(
            targetAccountCode = AccountConstants.CAPITAL,
            partnerPartyId = partner2.id,
            treasuryId = "TR_MAIN_YER",
            fiscalYear = fiscalYear,
            dateEpochDay = jan1,
            amountOrigMinor = 40_000_000L,
            currency = CurrencyCode.YER,
            exchangeRate = rate
        )

        // Generate Statement of Changes in Equity
        val fromEpoch = LocalDate.of(2026, 1, 1).toEpochDay()
        val toEpoch = LocalDate.of(2026, 1, 31).toEpochDay()
        val equityReport = generateStatementOfChangesInEquityReport(
            db = db,
            fromEpochDay = fromEpoch,
            toEpochDay = toEpoch
        )

        // Assert 4 columns and proper grand totals
        assertNotNull(equityReport)
        val closingRow = equityReport.rows.first { it.rowType == EquityChangeRowType.CLOSING_BALANCE }
        assertEquals(100_000_000L, closingRow.capitalMinor)
        assertEquals(0L, closingRow.currentMinor)
        assertEquals(100_000_000L, closingRow.totalMinor)

        // Reconciliation
        assertTrue("Statement of Changes in Equity must be 100% reconciled", equityReport.reconciliation.reconciled)
        assertEquals(0L, equityReport.reconciliation.totalDiscrepancyMinor)
    }

    @Test
    fun testOrphanLineHandlingMustFailReconciliation() = runBlocking {
        val fiscalYear = 2026
        val partner = PartyEntity(
            id = "PARTNER_VALID",
            name = "شريك معتمد",
            isPartner = true,
            equityPercentageBasisPoints = 10000
        )
        db.partyDao().insertParty(partner)
        db.treasuryDao().setAllowNegative("TR_MAIN_YER", true)

        val jan1 = LocalDate.of(2026, 1, 1).toEpochDay()
        val rate = ExchangeRate.parity(CurrencyCode.YER)

        // Legitimate capital receipt
        ledgerWriter.postCapitalReceipt(
            targetAccountCode = AccountConstants.CAPITAL,
            partnerPartyId = partner.id,
            treasuryId = "TR_MAIN_YER",
            fiscalYear = fiscalYear,
            dateEpochDay = jan1,
            amountOrigMinor = 50_000_000L,
            currency = CurrencyCode.YER,
            exchangeRate = rate
        )

        // Artificially inject an orphan line in 3201 (partner current) with null partyId
        val orphanDocId = UuidUtils.newTimeOrderedId()
        val orphanEntryId = UuidUtils.newTimeOrderedId()
        db.journalDao().insertEntry(
            JournalEntryEntity(
                id = orphanEntryId,
                docId = orphanDocId,
                entryNumber = 999999L,
                entryDateEpochDay = jan1 + 2,
                type = "NORMAL",
                memo = "قيد يتيم بدون شريك"
            )
        )
        db.journalDao().insertLines(
            listOf(
                JournalLineEntity(
                    id = UuidUtils.newTimeOrderedId(),
                    entryId = orphanEntryId,
                    lineNo = 1,
                    accountCode = AccountConstants.CASH_VAULT,
                    treasuryId = "TR_MAIN_YER",
                    origMinor = 10_000_000L,
                    currency = "YER",
                    exchangeRateMicros = rate.rateMicros,
                    baseDebitMinor = 10_000_000L,
                    baseCreditMinor = 0L,
                    memo = "طرف مدين"
                ),
                JournalLineEntity(
                    id = UuidUtils.newTimeOrderedId(),
                    entryId = orphanEntryId,
                    lineNo = 2,
                    accountCode = AccountConstants.PARTNER_CURRENT,
                    partyId = null, // ORPHAN LINE
                    origMinor = 10_000_000L,
                    currency = "YER",
                    exchangeRateMicros = rate.rateMicros,
                    baseDebitMinor = 0L,
                    baseCreditMinor = 10_000_000L,
                    memo = "قيد دائن جاري بدون تحديد شريك"
                )
            )
        )

        // Generate report
        val equityReport = generateStatementOfChangesInEquityReport(
            db = db,
            fromEpochDay = jan1,
            toEpochDay = jan1 + 10
        )

        // Reconciliation MUST fail because orphan lines exist in equity accounts
        assertFalse("Reconciliation MUST fail when orphan line exists in equity", equityReport.reconciliation.reconciled)

        val orphanCheck = equityReport.reconciliation.checks.firstOrNull { it.name.contains("Orphan") || it.name.contains("معلقة") || it.name.contains("أيتام") }
        assertNotNull("Orphan check must be present in checks list", orphanCheck)
        assertFalse("Orphan check must fail", orphanCheck!!.passed)
        assertEquals("Discrepancy must match orphan amount", 10_000_000L, orphanCheck.actualMinor)
    }

    @Test
    fun testMultiPartnerEquitySharingAndDividendsDistribution() = runBlocking {
        val fiscalYear = 2026
        val partner1 = PartyEntity(
            id = "PARTNER_A",
            name = "الشريك أ (مؤسس)",
            isPartner = true,
            equityPercentageBasisPoints = 7000 // 70%
        )
        val partner2 = PartyEntity(
            id = "PARTNER_B",
            name = "الشريك ب (مستثمر)",
            isPartner = true,
            equityPercentageBasisPoints = 3000 // 30%
        )
        db.partyDao().insertParty(partner1)
        db.partyDao().insertParty(partner2)
        db.treasuryDao().setAllowNegative("TR_MAIN_YER", true)

        val jan1 = LocalDate.of(2026, 1, 1).toEpochDay()
        val rate = ExchangeRate.parity(CurrencyCode.YER)

        // Capital contributions: 70M and 30M
        ledgerWriter.postCapitalReceipt(
            targetAccountCode = AccountConstants.CAPITAL,
            partnerPartyId = partner1.id,
            treasuryId = "TR_MAIN_YER",
            fiscalYear = fiscalYear,
            dateEpochDay = jan1,
            amountOrigMinor = 70_000_000L,
            currency = CurrencyCode.YER,
            exchangeRate = rate
        )
        ledgerWriter.postCapitalReceipt(
            targetAccountCode = AccountConstants.CAPITAL,
            partnerPartyId = partner2.id,
            treasuryId = "TR_MAIN_YER",
            fiscalYear = fiscalYear,
            dateEpochDay = jan1,
            amountOrigMinor = 30_000_000L,
            currency = CurrencyCode.YER,
            exchangeRate = rate
        )

        // Seed Retained Earnings (3301) for previous year: 20M YER
        val entryId = UuidUtils.newTimeOrderedId()
        db.journalDao().insertEntry(
            JournalEntryEntity(
                id = entryId,
                docId = UuidUtils.newTimeOrderedId(),
                entryNumber = 88888L,
                entryDateEpochDay = jan1 - 5,
                type = "NORMAL",
                memo = "أرباح مدورة مرحلة من 2025"
            )
        )
        db.journalDao().insertLines(
            listOf(
                JournalLineEntity(
                    id = UuidUtils.newTimeOrderedId(),
                    entryId = entryId,
                    lineNo = 1,
                    accountCode = AccountConstants.CASH_VAULT,
                    treasuryId = "TR_MAIN_YER",
                    origMinor = 20_000_000L,
                    currency = "YER",
                    exchangeRateMicros = rate.rateMicros,
                    baseDebitMinor = 20_000_000L,
                    baseCreditMinor = 0L,
                    memo = "صندوق النقدية أرباح مرحلة"
                ),
                JournalLineEntity(
                    id = UuidUtils.newTimeOrderedId(),
                    entryId = entryId,
                    lineNo = 2,
                    accountCode = AccountConstants.RETAINED_EARNINGS,
                    partyId = null,
                    origMinor = 20_000_000L,
                    currency = "YER",
                    exchangeRateMicros = rate.rateMicros,
                    baseDebitMinor = 0L,
                    baseCreditMinor = 20_000_000L,
                    memo = "أرباح مدورة مرحلة"
                )
            )
        )

        // Distribute 10M Dividends from 3301: 7M to partner 1, 3M to partner 2
        val jan15 = LocalDate.of(2026, 1, 15).toEpochDay()
        ledgerWriter.distributeDividends(
            totalDividendMinor = 10_000_000L,
            partnerShares = listOf(
                PartnerDividendSpec(partner1.id, partner1.name, 7_000_000L),
                PartnerDividendSpec(partner2.id, partner2.name, 3_000_000L)
            ),
            fiscalYear = fiscalYear,
            dateEpochDay = jan15,
            notes = "توزيع أرباح الربع الأول"
        )

        val fromEpoch = LocalDate.of(2026, 1, 1).toEpochDay()
        val toEpoch = LocalDate.of(2026, 1, 31).toEpochDay()
        val report = generateStatementOfChangesInEquityReport(db, fromEpoch, toEpoch)

        assertTrue("Report must be reconciled", report.reconciliation.reconciled)

        // Retained earnings opening: 20M, distributed: 10M, closing: 10M
        val distRow = report.rows.first { it.rowType == EquityChangeRowType.PROFIT_DISTRIBUTIONS }
        assertEquals(-10_000_000L, distRow.retainedEarningsMinor)
        assertEquals(10_000_000L, distRow.currentMinor)
        assertEquals(0L, distRow.totalMinor) // Internal equity transfer

        val closeRow = report.rows.first { it.rowType == EquityChangeRowType.CLOSING_BALANCE }
        assertEquals(100_000_000L, closeRow.capitalMinor)
        assertEquals(10_000_000L, closeRow.currentMinor)
        assertEquals(10_000_000L, closeRow.retainedEarningsMinor)
        assertEquals(120_000_000L, closeRow.totalMinor)

        // Partner summaries
        val p1Summary = report.partnerSummaries.first { it.partnerPartyId == partner1.id }
        val p2Summary = report.partnerSummaries.first { it.partnerPartyId == partner2.id }
        assertEquals(7_000_000L, p1Summary.dividendsReceivedMinor)
        assertEquals(3_000_000L, p2Summary.dividendsReceivedMinor)
        assertEquals(7_000_000L, p1Summary.currentClosingMinor)
        assertEquals(3_000_000L, p2Summary.currentClosingMinor)
    }
}
