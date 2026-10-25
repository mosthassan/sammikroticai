# Changelog - SamMikrotik

## [Prompt Fix 1 - Accounting Core Repair] - 2026-10-11

### 1. Unified Fiscal Calendar and Application Clock
- Added `FiscalCalendar` in `com.example.core.time` to handle arbitrary fiscal year starting months (`fiscalYearStartMonth`), leap years, epoch days, and period calculations without external dependencies.
- Added `AppClock` and `FixedAppClock` in `com.example.core.time` defaulting to the `Asia/Aden` (UTC+3) timezone, allowing reproducible time and date handling across the app and testing environments.
- Replaced all non-standard and hardcoded date calculations (e.g. `System.currentTimeMillis() / 86400000`, `20365L`, and unadjusted `LocalDate.now()`) with `AppClock.todayEpochDay()` and `FiscalCalendar.yearEnd(fiscalYear)`.

### 2. Year-End Closing Redesign & Balance Sheet Equilibrium
- Enhanced `LedgerWriter.executeYearEndClosing` and `PostingRules.createClosingDraftFromNetBalances`:
  - Properly clears nominal accounts (4xxx revenues, 5xxx expenses), including contra-accounts (debit balances in 4xxx and credit balances in 5xxx).
  - Automatically routes net income to Retained Earnings (`3301`).
  - Closes all 12 periods of the fiscal year in `fiscal_periods`.
  - Determines the closing date dynamically via `FiscalCalendar.yearEnd(fiscalYear)`.
- Replaced hardcoded closing date `20365L` in `AppViewModel.executeYearEndClosing` with `FiscalCalendar.yearEnd(fiscalYear)`.
- Verified Balance Sheet invariant: `Assets == Liabilities + Equity (Capital + Partner Current + Retained Earnings + Unclosed Profit)`.

### 3. Journal Entry Sequence & Unique Constraint Guard
- Updated `persistJournalDraft` in `LedgerWriter`:
  - Systematically allocates a unique, sequential `entryNumber` using `allocateNextDocNumber("JOURNAL_ENTRY", 0)` to prevent uniqueness collisions across different document types and reversal entries.
  - Supports `reversalOfEntryId` for explicit lineage linking on voiding and reversing entries.
- Updated `BackupRestoreUseCase` to include `revision` on documents and `reversalOfEntryId` on journal entries.

### 4. Document Amendment Workflow
- Added `amendSalesInvoice` to `LedgerWriter`:
  - Automatically reverses the original document with a compensating `REVERSAL` journal entry.
  - Marks the original document as `VOIDED`.
  - Reverses stock movements and enforces negative stock protection.
  - Creates a new `POSTED` document linking `reversalOfDocId` to the original document, incrementing `revision = originalDoc.revision + 1`.
  - Atomically executed within a single Room database transaction.

### 5. Invariant Validation Optimization
- Integrated lightweight $O(1)$ single-entry validation `invariants.verifyEntry(entryId)` immediately upon persisting each journal entry in production.
- Kept full ledger verification `invariants.verifyAll()` guarded by `enableInvariantValidation`, which defaults to `BuildConfig.DEBUG`.

### 6. Partner Equity Fixed Agreed Mode Fix
- Fixed `PartnerEquityUseCase.setEquityShareMode` to gracefully handle uninitialized organizations by creating/updating the record, preventing fallback failures in fixed agreed equity mode tests.

## [Phase 2.5 - Financial Reports Suite & Export Engine] - 2026-10-25

### 1. Report 1: Monthly Profit Breakdown (12 Months)
- Built multi-period ISP profitability analysis isolating direct bandwidth capacity costing (account 5101) from operational overheads.
- Integrated Hare-Niemeyer largest-remainder algorithm for zero-rounding partner dividend allocations.
- Real-time comparison versus previous month and same month of previous year with basis-point margin analytics.

### 2. Report 2: Statement of Changes in Equity (IFRS)
- Implemented 5-column equity movement matrix across Share Capital (`3101`), Partner Current Accounts (`3201`), Retained Earnings (`3301`), and Period Net Profit (`3302`).
- Tracked cash and in-kind capital contributions, drawings, and profit distributions with full mathematical continuity.

### 3. Report 3: Cash Flow Statement (IAS 7 Direct Method)
- Direct cash flow classification for all cash and electronic wallet movements into Operating, Investing, and Financing activities.
- Handled compound multi-item invoice splits using Hare-Niemeyer integer arithmetic.
- Isolated inter-treasury transfers (zero net impact) and disclosed non-cash financing/investing transactions in the notes.

### 4. Report 4: Daily Cash Register (سجل الصندوق اليومي)
- Implemented chronological real-time transaction ledger for individual treasuries and currencies with dual running balances (original currency and base currency YER).
- Added informational market valuation estimate note: `"تقدير بسعر اليوم، لا يدخل في أي قيد"`.
- Dual reconciliation verification against physical cash balances and GL control account balances.

### 5. Unified Reports Export Engine (PDF & CSV)
- Added `ReportsCsvGenerator`: Generates UTF-8 with BOM (`\uFEFF`) CSV files with standard decimal values (period separator, zero thousands commas) using integer arithmetic (zero Double/Float).
- Added `ReportsPdfGenerator`: Pixel-perfect A4 PDF generation with StaticLayout RTL Arabic text rendering, dark navy banners, cyan accents, KPI overview cards, alternating rows, and dynamic reconciliation badges.
- Added `ReportExportManager`: Secure native Android share chooser and external viewer wrapper leveraging Jetpack `FileProvider`.
- Added interactive export buttons to all 4 report screens (`ReportExportButtons`).

