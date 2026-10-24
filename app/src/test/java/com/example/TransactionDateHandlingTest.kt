package com.example

import com.example.data.local.entity.AllocationEntity
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.CurrencyRateEntity
import com.example.data.local.entity.DocumentEntity
import com.example.data.local.entity.IdempotencyKeyEntity
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.JournalEntryEntity
import com.example.data.local.entity.PartyEntity
import com.example.data.local.entity.PurchaseEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.data.local.entity.VoucherEntity
import com.example.util.TransactionDateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TransactionDateHandlingTest {

    @Test
    fun testTransactionDateUtilsPatternAndMonthFormatting() {
        val format = TransactionDateUtils.createDateFormat()
        assertEquals("yyyy/MM/dd", format.toPattern())

        val dtFormat = TransactionDateUtils.createDateTimeFormat()
        assertEquals("yyyy/MM/dd HH:mm", dtFormat.toPattern())

        // Test epoch day formatting: epochDay 19723 = 2024-01-01
        val epochDay = LocalDate.of(2025, 10, 24).toEpochDay()
        val formattedEpochDay = TransactionDateUtils.formatEpochDay(epochDay)
        assertEquals("2025/10/24", formattedEpochDay)

        // Ensure month is uppercase '10', NOT minute 'mm'
        assertTrue("Formatted date must contain /10/", formattedEpochDay.contains("/10/"))
    }

    @Test
    fun testDynamicTimestampDefaults() {
        val now = System.currentTimeMillis()

        val doc = DocumentEntity(
            id = "test-doc-1",
            type = "SALES_INVOICE",
            fiscalYear = 2026,
            docNumber = 1L,
            partyId = "party-1",
            dateEpochDay = 19723L,
            currency = "YER",
            exchangeRateMicros = 1_000_000L,
            totalMinor = 100_000L,
            totalBaseMinor = 100_000L,
            status = "POSTED"
        )
        assertTrue("DocumentEntity createdAt must be >= before instantiation", doc.createdAt >= now)

        val entry = JournalEntryEntity(
            id = "entry-1",
            docId = "doc-1",
            entryNumber = 1L,
            entryDateEpochDay = 19723L,
            type = "NORMAL",
            memo = "Test entry"
        )
        assertTrue("JournalEntryEntity createdAt must be >= before instantiation", entry.createdAt >= now)

        val audit = AuditLogEntity(
            id = "audit-1",
            entityType = "DOCUMENT",
            entityId = "doc-1",
            action = "POST"
        )
        assertTrue("AuditLogEntity timestamp must be >= before instantiation", audit.timestamp >= now)

        val alloc = AllocationEntity(
            id = "alloc-1",
            paymentDocId = "pay-1",
            invoiceDocId = "inv-1",
            allocatedOrigMinor = 50_000L,
            allocatedBaseMinor = 50_000L
        )
        assertTrue("AllocationEntity createdAt must be >= before instantiation", alloc.createdAt >= now)

        val stock = StockMovementEntity(
            id = "stock-1",
            packageId = "pkg-1",
            docId = "doc-1",
            type = "SELL",
            quantity = 10,
            movementDateEpochDay = 19723L
        )
        assertTrue("StockMovementEntity createdAt must be >= before instantiation", stock.createdAt >= now)

        val party = PartyEntity(
            id = "party-1",
            name = "Test Party"
        )
        assertTrue("PartyEntity createdAt must be >= before instantiation", party.createdAt >= now)

        val rate = CurrencyRateEntity(
            id = "rate-1",
            currency = "SAR",
            rateMicros = 365_000_000L,
            effectiveDateEpochDay = 19723L
        )
        assertTrue("CurrencyRateEntity createdAt must be >= before instantiation", rate.createdAt >= now)

        val key = IdempotencyKeyEntity(
            key = "key-1",
            docId = "doc-1"
        )
        assertTrue("IdempotencyKeyEntity createdAt must be >= before instantiation", key.createdAt >= now)
    }

    @Test
    fun testModelTypealiases() {
        val invoice: InvoiceEntity = DocumentEntity(
            id = "inv-1",
            type = "SALES_INVOICE",
            fiscalYear = 2026,
            docNumber = 1L,
            partyId = "p-1",
            dateEpochDay = 19723L,
            currency = "YER",
            exchangeRateMicros = 1_000_000L,
            totalMinor = 50_000L,
            totalBaseMinor = 50_000L,
            status = "POSTED",
            createdAt = System.currentTimeMillis()
        )
        assertEquals("SALES_INVOICE", invoice.type)

        val purchase: PurchaseEntity = DocumentEntity(
            id = "pur-1",
            type = "PURCHASE_INVOICE",
            fiscalYear = 2026,
            docNumber = 2L,
            partyId = "p-2",
            dateEpochDay = 19723L,
            currency = "YER",
            exchangeRateMicros = 1_000_000L,
            totalMinor = 80_000L,
            totalBaseMinor = 80_000L,
            status = "POSTED",
            createdAt = System.currentTimeMillis()
        )
        assertEquals("PURCHASE_INVOICE", purchase.type)

        val voucher: VoucherEntity = DocumentEntity(
            id = "vouch-1",
            type = "RECEIPT_VOUCHER",
            fiscalYear = 2026,
            docNumber = 3L,
            partyId = "p-1",
            dateEpochDay = 19723L,
            currency = "YER",
            exchangeRateMicros = 1_000_000L,
            totalMinor = 20_000L,
            totalBaseMinor = 20_000L,
            status = "POSTED",
            createdAt = System.currentTimeMillis()
        )
        assertEquals("RECEIPT_VOUCHER", voucher.type)
    }
}
