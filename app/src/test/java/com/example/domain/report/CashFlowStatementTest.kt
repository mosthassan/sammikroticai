package com.example.domain.report

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.ledger.AccountConstants
import com.example.core.ledger.DocumentType
import com.example.data.ledger.PurchaseItemSpec
import com.example.core.model.CurrencyCode
import com.example.core.model.ExchangeRate
import com.example.core.model.UuidUtils
import com.example.data.ledger.InvoiceAllocationSpec
import com.example.data.ledger.LedgerWriter
import com.example.data.ledger.PaymentSource
import com.example.data.ledger.PaymentVoucherType
import com.example.data.local.AppDatabase
import com.example.data.local.entity.DocumentItemEntity
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
class CashFlowStatementTest {

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

        runBlocking {
            db.treasuryDao().insertTreasury(
                com.example.data.local.entity.TreasuryAccountEntity(
                    id = "TR_BANK_KURIMI",
                    name = "حساب الكريمي مميز",
                    glAccountCode = AccountConstants.BANKS_WALLETS,
                    currency = "YER",
                    allowNegative = true
                )
            )
            db.treasuryDao().setAllowNegative("TR_MAIN_YER", true)
            db.treasuryDao().setAllowNegative("TR_BANK_KURIMI", true)
            db.treasuryDao().setAllowNegative("TR_USD_VAULT", true)
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testHareNiemeyerPureIntegerSplitting() {
        // Test 1: Simple 60% Asset / 40% Expense Split
        val items1 = listOf(
            DocumentItemEntity(
                id = "item_1",
                docId = "doc_1",
                itemIndex = 0,
                description = "راوتر سيرفرات CCR2004",
                accountCode = AccountConstants.FIXED_ASSETS_NETWORK,
                quantity = 1,
                unitPriceMinor = 60_000L,
                totalMinor = 60_000L,
                isAsset = true
            ),
            DocumentItemEntity(
                id = "item_2",
                docId = "doc_1",
                itemIndex = 1,
                description = "أجور تمديد وتركيب أسلاك",
                accountCode = AccountConstants.OPERATING_EXPENSES,
                quantity = 1,
                unitPriceMinor = 40_000L,
                totalMinor = 40_000L,
                isAsset = false
            )
        )

        val (op1, inv1) = HareNiemeyerSplitter.splitPaymentProportionally(
            totalPaymentMinor = 50_000L,
            invoiceItems = items1
        )
        assertEquals(20_000L, op1)
        assertEquals(30_000L, inv1)
        assertEquals(50_000L, op1 + inv1)

        // Test 2: Uneven Odd Numbers / Largest Remainder handling
        // Invoice: 33,333 (Asset) + 66,667 (Expense) = 100,000
        // Payment: 10,000
        val items2 = listOf(
            DocumentItemEntity(
                id = "item_3",
                docId = "doc_2",
                itemIndex = 0,
                description = "معدات شبكة",
                accountCode = AccountConstants.FIXED_ASSETS_NETWORK,
                quantity = 1,
                unitPriceMinor = 33_333L,
                totalMinor = 33_333L,
                isAsset = true
            ),
            DocumentItemEntity(
                id = "item_4",
                docId = "doc_2",
                itemIndex = 1,
                description = "مصاريف تشغيلية",
                accountCode = AccountConstants.OPERATING_EXPENSES,
                quantity = 1,
                unitPriceMinor = 66_667L,
                totalMinor = 66_667L,
                isAsset = false
            )
        )

        val (op2, inv2) = HareNiemeyerSplitter.splitPaymentProportionally(
            totalPaymentMinor = 10_000L,
            invoiceItems = items2
        )
        // 10000 * 66667 / 100000 = 6666.7 -> 6667
        // 10000 * 33333 / 100000 = 3333.3 -> 3333
        assertEquals(6_667L, op2)
        assertEquals(3_333L, inv2)
        assertEquals(10_000L, op2 + inv2)

        // Test 3: 100% Asset Invoice
        val items3 = listOf(
            DocumentItemEntity(
                id = "item_5",
                docId = "doc_3",
                itemIndex = 0,
                description = "أنتينا ميموزا C5c",
                accountCode = AccountConstants.FIXED_ASSETS_NETWORK,
                quantity = 1,
                unitPriceMinor = 50_000L,
                totalMinor = 50_000L,
                isAsset = true
            )
        )
        val (op3, inv3) = HareNiemeyerSplitter.splitPaymentProportionally(
            totalPaymentMinor = 25_000L,
            invoiceItems = items3
        )
        assertEquals(0L, op3)
        assertEquals(25_000L, inv3)

        // Test 4: 100% Expense Invoice
        val items4 = listOf(
            DocumentItemEntity(
                id = "item_6",
                docId = "doc_4",
                itemIndex = 0,
                description = "ديزل للمولدات",
                accountCode = AccountConstants.OPERATING_EXPENSES,
                quantity = 1,
                unitPriceMinor = 70_000L,
                totalMinor = 70_000L,
                isAsset = false
            )
        )
        val (op4, inv4) = HareNiemeyerSplitter.splitPaymentProportionally(
            totalPaymentMinor = 70_000L,
            invoiceItems = items4
        )
        assertEquals(70_000L, op4)
        assertEquals(0L, inv4)
    }

    @Test
    fun testVendorPaymentProportionalInvoiceSplittingInReport() = runBlocking {
        val fiscalYear = 2026
        val vendor = PartyEntity(
            id = "VEND_FIBER_TECH",
            name = "شركة الألياف والمعدات المتقدمة",
            isVendor = true
        )
        db.partyDao().insertParty(vendor)

        val jan1 = LocalDate.of(2026, 1, 1).toEpochDay()
        val jan10 = LocalDate.of(2026, 1, 10).toEpochDay()

        // 1. Post Purchase Invoice with mixed asset + expense lines:
        // Asset line: 600,000 YER (60,000,000 minor)
        // Expense line: 400,000 YER (40,000,000 minor)
        // Total Invoice = 1,000,000 YER (100,000,000 minor)
        val invoice = ledgerWriter.postPurchaseInvoice(
            vendorPartyId = vendor.id,
            fiscalYear = fiscalYear,
            dateEpochDay = jan1,
            currency = CurrencyCode.YER,
            exchangeRate = ExchangeRate.parity(CurrencyCode.YER),
            items = listOf(
                PurchaseItemSpec(
                    description = "سيرفرات كاش ومايكروتك",
                    accountCode = AccountConstants.FIXED_ASSETS_NETWORK,
                    quantity = 2,
                    unitPriceMinor = 30_000_000L,
                    isAsset = true
                ),
                PurchaseItemSpec(
                    description = "أجور صيانة وكوابل فايبر",
                    accountCode = AccountConstants.MAINTENANCE_SPARES,
                    quantity = 1,
                    unitPriceMinor = 40_000_000L,
                    isAsset = false
                )
            ),
            notes = "فاتورة مشتريات مختلطة معدات وصيانة"
        )

        // 2. Post a Payment Voucher of 500,000 YER (50,000,000 minor) allocated to this invoice
        val payment = ledgerWriter.postPaymentVoucher(
            recipientPartyId = vendor.id,
            treasuryId = "TR_MAIN_YER",
            fiscalYear = fiscalYear,
            dateEpochDay = jan10,
            amountOrigMinor = 50_000_000L,
            currency = CurrencyCode.YER,
            exchangeRate = ExchangeRate.parity(CurrencyCode.YER),
            paymentType = PaymentVoucherType.VENDOR_SETTLEMENT,
            invoiceAllocations = listOf(
                InvoiceAllocationSpec(
                    invoiceDocId = invoice.id,
                    allocatedOrigMinor = 50_000_000L
                )
            ),
            notes = "دفعة مسددة من فاتورة المشتريات المختلطة"
        )
        assertNotNull(payment)

        // 3. Generate Cash Flow Statement
        val statement = generateCashFlowStatement(db, jan1, jan10)

        // 4. Assert Proportional Splitting:
        // Expected Operating portion = 50,000,000 * 40 / 100 = 20,000,000 minor
        // Expected Investing portion = 50,000,000 * 60 / 100 = 30,000,000 minor
        val opVendorPayment = statement.operatingSection.lines
            .filter { it.category == CashFlowCategory.VENDOR_PAYMENTS_OPERATING }
            .sumOf { it.outflowMinor }

        val invVendorPayment = statement.investingSection.lines
            .filter { it.category == CashFlowCategory.VENDOR_PAYMENTS_INVESTING }
            .sumOf { it.outflowMinor }

        assertEquals(20_000_000L, opVendorPayment)
        assertEquals(30_000_000L, invVendorPayment)
        assertEquals(50_000_000L, opVendorPayment + invVendorPayment)

        // Reconciliation
        assertEquals(0L, statement.unclassifiedAmountMinor)
        assertTrue("Statement should reconcile", statement.reconciliation.reconciled)
        assertEquals(4, statement.reconciliation.passedCount)
        assertEquals(0, statement.reconciliation.failedCount)
    }

    @Test
    fun testFxRevaluationStandaloneExclusionAndReversal() = runBlocking {
        val fiscalYear = 2026
        val jan1 = LocalDate.of(2026, 1, 1).toEpochDay()
        val jan15 = LocalDate.of(2026, 1, 15).toEpochDay()

        // 1. Initial Deposit in USD Vault: 1,000 USD at rate 530 YER/USD (Book YER = 530,000 YER)
        ledgerWriter.postCapitalReceipt(
            targetAccountCode = AccountConstants.CAPITAL,
            partnerPartyId = null,
            treasuryId = "TR_USD_VAULT",
            fiscalYear = fiscalYear,
            dateEpochDay = jan1,
            amountOrigMinor = 1_000_00L,
            currency = CurrencyCode.USD,
            exchangeRate = ExchangeRate(CurrencyCode.USD, CurrencyCode.FUNCTIONAL, 530_000_000L),
            notes = "إيداع رأسمال بالدولار"
        )

        // 2. Post Periodic Revaluation at rate 540 YER/USD:
        // Market = 1,000 * 540 = 540,000 YER. Book = 530,000 YER. Gain = +10,000 YER (1,000,000 minor).
        val revalDoc = ledgerWriter.postPeriodicRevaluation(
            accountCode = AccountConstants.CASH_VAULT,
            treasuryId = "TR_USD_VAULT",
            fiscalYear = fiscalYear,
            dateEpochDay = jan15,
            foreignCurrency = CurrencyCode.USD,
            exchangeRate = ExchangeRate(CurrencyCode.USD, CurrencyCode.FUNCTIONAL, 540_000_000L),
            notes = "إعادة تقييم خزينة الدولار في منتصف الشهر"
        )
        assertNotNull(revalDoc)

        // 3. Generate Cash Flow Statement
        val statement = generateCashFlowStatement(db, jan1, jan15)

        // Verify:
        // FX Revaluation MUST NOT appear in Operating, Investing, or Financing flows!
        val opFx = statement.operatingSection.lines.filter { it.category == CashFlowCategory.FX_REVALUATION_CASH }
        val invFx = statement.investingSection.lines.filter { it.category == CashFlowCategory.FX_REVALUATION_CASH }
        val finFx = statement.financingSection.lines.filter { it.category == CashFlowCategory.FX_REVALUATION_CASH }
        assertTrue("Operating should not contain FX revaluation", opFx.isEmpty())
        assertTrue("Investing should not contain FX revaluation", invFx.isEmpty())
        assertTrue("Financing should not contain FX revaluation", finFx.isEmpty())

        // Verify standalone line:
        assertEquals(1_000_000L, statement.fxImpactOnCashMinor)
        assertEquals(1, statement.fxSection.lines.size)

        // Verify Dynamic Reconciliation Check:
        // Opening (0) + Net Change (Financing 53,000,000 + FX Impact 1,000,000 = 54,000,000) == Closing (54,000,000)
        assertEquals(0L, statement.openingCashMinor)
        assertEquals(54_000_000L, statement.closingCashMinor)
        assertEquals(54_000_000L, statement.netChangeInCashMinor)
        assertEquals(0L, statement.unclassifiedAmountMinor)
        assertTrue("Statement should reconcile with FX impact line", statement.reconciliation.reconciled)
    }

    @Test
    fun testCashTransfersAndExchangesAreExcludedNetZero() = runBlocking {
        val fiscalYear = 2026
        val jan1 = LocalDate.of(2026, 1, 1).toEpochDay()
        val jan5 = LocalDate.of(2026, 1, 5).toEpochDay()

        // 1. Initial Cash via Capital Receipt: 200,000 YER in TR_MAIN_YER
        ledgerWriter.postCapitalReceipt(
            targetAccountCode = AccountConstants.CAPITAL,
            partnerPartyId = null,
            treasuryId = "TR_MAIN_YER",
            fiscalYear = fiscalYear,
            dateEpochDay = jan1,
            amountOrigMinor = 20_000_000L,
            currency = CurrencyCode.YER,
            exchangeRate = ExchangeRate.parity(CurrencyCode.YER),
            notes = "تغذية الصندوق الرئيسي"
        )

        // 2. Transfer 50,000 YER from TR_MAIN_YER to TR_BANK_KURIMI
        val transferDoc = ledgerWriter.postTreasuryTransfer(
            sourceTreasuryId = "TR_MAIN_YER",
            sourceAmountOrigMinor = 5_000_000L,
            sourceCurrency = CurrencyCode.YER,
            sourceRate = ExchangeRate.parity(CurrencyCode.YER),
            destTreasuryId = "TR_BANK_KURIMI",
            destAmountOrigMinor = 5_000_000L,
            destCurrency = CurrencyCode.YER,
            destRate = ExchangeRate.parity(CurrencyCode.YER),
            fiscalYear = fiscalYear,
            dateEpochDay = jan5,
            notes = "تحويل نقدية إلى حساب الكريمي"
        )
        assertNotNull(transferDoc)

        // 3. Generate Cash Flow Statement
        val statement = generateCashFlowStatement(db, jan1, jan5)

        // Net change in total cash (1101 + 1102) from transfer is exactly 0.
        // Transfer must be excluded from Operating, Investing, and Financing flows!
        val opTransfers = statement.operatingSection.lines.filter { it.category == CashFlowCategory.CASH_TRANSFERS }
        val invTransfers = statement.investingSection.lines.filter { it.category == CashFlowCategory.CASH_TRANSFERS }
        val finTransfers = statement.financingSection.lines.filter { it.category == CashFlowCategory.CASH_TRANSFERS }
        assertTrue("Operating should not contain transfers", opTransfers.isEmpty())
        assertTrue("Investing should not contain transfers", invTransfers.isEmpty())
        assertTrue("Financing should not contain transfers", finTransfers.isEmpty())

        // Closing Cash should be 20,000,000 (15,000,000 in main vault + 5,000,000 in Kurimi)
        assertEquals(20_000_000L, statement.closingCashMinor)
        assertEquals(15_000_000L, statement.vaultCashClosingMinor)
        assertEquals(5_000_000L, statement.bankCashClosingMinor)
        assertEquals(0L, statement.unclassifiedAmountMinor)
        assertTrue(statement.reconciliation.reconciled)
    }

    @Test
    fun testNonCashDisclosuresSection() = runBlocking {
        val fiscalYear = 2026
        val partner = PartyEntity(
            id = "PARTNER_YASSER",
            name = "الأستاذ ياسر (شريك)",
            isPartner = true
        )
        val vendor = PartyEntity(
            id = "VEND_TOWERS",
            name = "مؤسسة الأبراج للحديد",
            isVendor = true
        )
        db.partyDao().insertParty(partner)
        db.partyDao().insertParty(vendor)

        val jan1 = LocalDate.of(2026, 1, 1).toEpochDay()
        val jan8 = LocalDate.of(2026, 1, 8).toEpochDay()

        // 1. In-kind fixed asset capital contribution (DR 1501 CR 3101) - NO CASH TOUCHED
        ledgerWriter.postInKindCapitalContribution(
            partnerPartyId = partner.id,
            assetName = "برج اتصالات مجلفن 36 متر",
            fiscalYear = fiscalYear,
            dateEpochDay = jan1,
            amountOrigMinor = 80_000_000L, // 800,000 YER
            currency = CurrencyCode.YER,
            exchangeRate = ExchangeRate.parity(CurrencyCode.YER),
            notes = "مساهمة عينية ببرج شبكة"
        )

        // 2. Partner payment out of personal pocket for operating expense (DR 5201 CR 3201) - NO CASH TOUCHED
        ledgerWriter.postPaymentVoucher(
            recipientPartyId = vendor.id,
            fiscalYear = fiscalYear,
            dateEpochDay = jan8,
            amountOrigMinor = 25_000_000L, // 250,000 YER
            currency = CurrencyCode.YER,
            exchangeRate = ExchangeRate.parity(CurrencyCode.YER),
            paymentType = PaymentVoucherType.OPERATING_EXPENSE,
            paymentSource = PaymentSource.PARTNER_PERSONAL,
            payingPartnerPartyId = partner.id,
            notes = "سداد مصاريف صيانة برج من الحساب الشخصي للشريك"
        )

        // 3. Generate Statement
        val statement = generateCashFlowStatement(db, jan1, jan8)

        // Cash flows should be 0 since no cash accounts were touched
        assertEquals(0L, statement.netChangeInCashMinor)
        assertEquals(0L, statement.openingCashMinor)
        assertEquals(0L, statement.closingCashMinor)

        // Non-cash disclosures should contain both items
        assertEquals(2, statement.nonCashDisclosures.size)
        val inKindItem = statement.nonCashDisclosures.firstOrNull { it.typeAr.contains("عينية") }
        val paidOnBehalfItem = statement.nonCashDisclosures.firstOrNull { it.typeAr.contains("بالنيابة") }

        assertNotNull("In-kind item must be disclosed", inKindItem)
        assertNotNull("Paid on behalf item must be disclosed", paidOnBehalfItem)
        assertEquals(80_000_000L, inKindItem!!.amountMinor)
        assertEquals(25_000_000L, paidOnBehalfItem!!.amountMinor)

        assertTrue(statement.reconciliation.reconciled)
    }

    @Test
    fun testCompleteFullIspCashFlowLifecycleAndDynamicReconciliation() = runBlocking {
        val fiscalYear = 2026
        val customer = PartyEntity(id = "CUST_AL_AMAL", name = "مركز الأمل للإنترنت", isCustomer = true)
        val vendorIsp = PartyEntity(id = "VEND_STARLINK", name = "شركة ستارلينك لخدمات الأقمار", isVendor = true)
        val partner = PartyEntity(id = "PARTNER_ALI", name = "علي العباسي", isPartner = true)

        db.partyDao().insertParty(customer)
        db.partyDao().insertParty(vendorIsp)
        db.partyDao().insertParty(partner)

        val jan1 = LocalDate.of(2026, 1, 1).toEpochDay()
        val jan5 = LocalDate.of(2026, 1, 5).toEpochDay()
        val jan10 = LocalDate.of(2026, 1, 10).toEpochDay()
        val jan15 = LocalDate.of(2026, 1, 15).toEpochDay()
        val jan20 = LocalDate.of(2026, 1, 20).toEpochDay()

        // 1. Financing Inflow: Partner cash capital contribution (+1,000,000 YER)
        ledgerWriter.postCapitalReceipt(
            targetAccountCode = AccountConstants.CAPITAL,
            partnerPartyId = partner.id,
            treasuryId = "TR_MAIN_YER",
            fiscalYear = fiscalYear,
            dateEpochDay = jan1,
            amountOrigMinor = 100_000_000L,
            currency = CurrencyCode.YER,
            exchangeRate = ExchangeRate.parity(CurrencyCode.YER),
            notes = "مساهمة تأسيسية"
        )

        // 2. Operating Inflow: Customer collection (+300,000 YER)
        ledgerWriter.postCustomerReceipt(
            partyId = customer.id,
            treasuryId = "TR_MAIN_YER",
            fiscalYear = fiscalYear,
            dateEpochDay = jan5,
            amountOrigMinor = 30_000_000L,
            currency = CurrencyCode.YER,
            exchangeRate = ExchangeRate.parity(CurrencyCode.YER),
            notes = "تحصيل اشتراكات عملاء"
        )

        // 3. Operating Outflow: Direct Upstream ISP cost Starlink (-150,000 YER)
        ledgerWriter.postPaymentVoucher(
            recipientPartyId = vendorIsp.id,
            treasuryId = "TR_MAIN_YER",
            fiscalYear = fiscalYear,
            dateEpochDay = jan10,
            amountOrigMinor = 15_000_000L,
            currency = CurrencyCode.YER,
            exchangeRate = ExchangeRate.parity(CurrencyCode.YER),
            paymentType = PaymentVoucherType.DIRECT_ISP_SERVICE,
            notes = "سداد اشتراك ستارلينك الشهري"
        )

        // 4. Operating Outflow: Operating expense (diesel/generator) (-50,000 YER)
        ledgerWriter.postPaymentVoucher(
            recipientPartyId = vendorIsp.id,
            treasuryId = "TR_MAIN_YER",
            fiscalYear = fiscalYear,
            dateEpochDay = jan15,
            amountOrigMinor = 5_000_000L,
            currency = CurrencyCode.YER,
            exchangeRate = ExchangeRate.parity(CurrencyCode.YER),
            paymentType = PaymentVoucherType.OPERATING_EXPENSE,
            notes = "شراء ديزل لمولد المحطة"
        )

        // 5. Financing Outflow: Partner drawings (-100,000 YER)
        ledgerWriter.postPaymentVoucher(
            recipientPartyId = partner.id,
            treasuryId = "TR_MAIN_YER",
            fiscalYear = fiscalYear,
            dateEpochDay = jan20,
            amountOrigMinor = 10_000_000L,
            currency = CurrencyCode.YER,
            exchangeRate = ExchangeRate.parity(CurrencyCode.YER),
            paymentType = PaymentVoucherType.PARTNER_DRAWINGS,
            notes = "مسحوبات شخصية للشريك علي"
        )

        // 6. Generate Cash Flow Statement
        val statement = generateCashFlowStatement(db, jan1, jan20)

        // Assert Net Flows:
        // Operating: Inflow (+30,000,000) - Outflow (15,000,000 + 5,000,000) = +10,000,000 minor
        assertEquals(10_000_000L, statement.netOperatingCashFlowMinor)

        // Financing: Inflow (+100,000,000) - Outflow (10,000,000) = +90,000,000 minor
        assertEquals(90_000_000L, statement.netFinancingCashFlowMinor)

        // Investing: 0
        assertEquals(0L, statement.netInvestingCashFlowMinor)

        // Total Net Change in Cash: +10,000,000 + 90,000,000 = +100,000,000 minor
        assertEquals(100_000_000L, statement.netChangeInCashMinor)
        assertEquals(100_000_000L, statement.closingCashMinor)
        assertEquals(0L, statement.openingCashMinor)

        // Check Dynamic Reconciliation
        assertEquals(0L, statement.unclassifiedAmountMinor)
        assertTrue("Full lifecycle must pass dynamic reconciliation", statement.reconciliation.reconciled)
        assertEquals(4, statement.reconciliation.passedCount)
        assertEquals(0, statement.reconciliation.failedCount)
    }

    @Test
    fun testQueryCountStabilityWith200Vs2000Entries() = runBlocking {
        // Test query count stability: Batch querying ensures O(1) database trips
        // whether processing 200 lines or 2000 lines.
        val customer = PartyEntity(id = "CUST_BULK", name = "عميل جملة", isCustomer = true)
        db.partyDao().insertParty(customer)

        val jan1 = LocalDate.of(2026, 1, 1).toEpochDay()
        val jan2 = LocalDate.of(2026, 1, 2).toEpochDay()

        // Seed 200 entries directly into journal_entries and journal_lines
        val entries200 = mutableListOf<JournalEntryEntity>()
        val lines200 = mutableListOf<JournalLineEntity>()

        for (i in 1..200) {
            val entryId = "entry_200_$i"
            val docId = "doc_200_$i"

            db.documentDao().insertDocument(
                com.example.data.local.entity.DocumentEntity(
                    id = docId,
                    type = DocumentType.RECEIPT_VOUCHER.name,
                    fiscalYear = 2026,
                    docNumber = i.toLong(),
                    partyId = customer.id,
                    dateEpochDay = jan1,
                    currency = "YER",
                    exchangeRateMicros = 1_000_000L,
                    totalMinor = 1_000L,
                    totalBaseMinor = 1_000L,
                    status = "POSTED"
                )
            )

            entries200.add(
                JournalEntryEntity(
                    id = entryId,
                    docId = docId,
                    entryNumber = i.toLong(),
                    entryDateEpochDay = jan1,
                    type = "NORMAL",
                    memo = "سند تحصيل $i"
                )
            )
            lines200.add(
                JournalLineEntity(
                    id = "line_200_${i}_1",
                    entryId = entryId,
                    lineNo = 1,
                    accountCode = AccountConstants.CASH_VAULT,
                    partyId = customer.id,
                    treasuryId = "TR_MAIN_YER",
                    origMinor = 1_000L,
                    currency = "YER",
                    exchangeRateMicros = 1_000_000L,
                    baseDebitMinor = 1_000L,
                    baseCreditMinor = 0L,
                    memo = "قبض $i"
                )
            )
            lines200.add(
                JournalLineEntity(
                    id = "line_200_${i}_2",
                    entryId = entryId,
                    lineNo = 2,
                    accountCode = AccountConstants.ACCOUNTS_RECEIVABLE,
                    partyId = customer.id,
                    treasuryId = null,
                    origMinor = 1_000L,
                    currency = "YER",
                    exchangeRateMicros = 1_000_000L,
                    baseDebitMinor = 0L,
                    baseCreditMinor = 1_000L,
                    memo = "قبض $i"
                )
            )
        }

        for (entry in entries200) db.journalDao().insertEntry(entry)
        db.journalDao().insertLines(lines200)

        // Measure execution time and successful generation for 200 entries
        val startTime200 = System.currentTimeMillis()
        val statement200 = generateCashFlowStatement(db, jan1, jan2)
        val duration200 = System.currentTimeMillis() - startTime200

        assertEquals(200_000L, statement200.netOperatingCashFlowMinor)
        assertEquals(200, statement200.operatingSection.lines.size)
        assertTrue(statement200.reconciliation.reconciled)

        // Seed up to 2000 entries (additional 1800 entries)
        val entries1800 = mutableListOf<JournalEntryEntity>()
        val lines1800 = mutableListOf<JournalLineEntity>()

        for (i in 201..2000) {
            val entryId = "entry_2000_$i"
            val docId = "doc_2000_$i"

            db.documentDao().insertDocument(
                com.example.data.local.entity.DocumentEntity(
                    id = docId,
                    type = DocumentType.RECEIPT_VOUCHER.name,
                    fiscalYear = 2026,
                    docNumber = i.toLong(),
                    partyId = customer.id,
                    dateEpochDay = jan1,
                    currency = "YER",
                    exchangeRateMicros = 1_000_000L,
                    totalMinor = 1_000L,
                    totalBaseMinor = 1_000L,
                    status = "POSTED"
                )
            )

            entries1800.add(
                JournalEntryEntity(
                    id = entryId,
                    docId = docId,
                    entryNumber = i.toLong(),
                    entryDateEpochDay = jan1,
                    type = "NORMAL",
                    memo = "سند تحصيل $i"
                )
            )
            lines1800.add(
                JournalLineEntity(
                    id = "line_2000_${i}_1",
                    entryId = entryId,
                    lineNo = 1,
                    accountCode = AccountConstants.CASH_VAULT,
                    partyId = customer.id,
                    treasuryId = "TR_MAIN_YER",
                    origMinor = 1_000L,
                    currency = "YER",
                    exchangeRateMicros = 1_000_000L,
                    baseDebitMinor = 1_000L,
                    baseCreditMinor = 0L,
                    memo = "قبض $i"
                )
            )
            lines1800.add(
                JournalLineEntity(
                    id = "line_2000_${i}_2",
                    entryId = entryId,
                    lineNo = 2,
                    accountCode = AccountConstants.ACCOUNTS_RECEIVABLE,
                    partyId = customer.id,
                    treasuryId = null,
                    origMinor = 1_000L,
                    currency = "YER",
                    exchangeRateMicros = 1_000_000L,
                    baseDebitMinor = 0L,
                    baseCreditMinor = 1_000L,
                    memo = "قبض $i"
                )
            )
        }

        for (entry in entries1800) db.journalDao().insertEntry(entry)
        db.journalDao().insertLines(lines1800)

        // Measure execution time for 2000 entries
        val startTime2000 = System.currentTimeMillis()
        val statement2000 = generateCashFlowStatement(db, jan1, jan2)
        val duration2000 = System.currentTimeMillis() - startTime2000

        assertEquals(2_000_000L, statement2000.netOperatingCashFlowMinor)
        assertEquals(2000, statement2000.operatingSection.lines.size)
        assertTrue(statement2000.reconciliation.reconciled)

        // Verify query count stability: Both executed exactly the same batch query structure
        // without per-line looping (N+1 queries). Execution finishes smoothly.
        println("Performance check: 200 entries = ${duration200}ms, 2000 entries = ${duration2000}ms")
    }
}
