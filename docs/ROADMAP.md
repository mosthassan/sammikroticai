# SamMikrotik - Product & Engineering Roadmap

## Phase 1: Accounting Core & Architecture (Current)
- [x] Comprehensive Documentation (`AGENTS.md`, Architecture, Accounting Spec, Design System, Test Plan, Decisions).
- [x] Zero-Float Money Engine (`Money`, `CurrencyCode`, `ExchangeRate`, integer micro-rates, rounding distribution).
- [x] Room Database v1 Schema (Organizations, Parties, Treasuries, Chart of Accounts, Periods, Journal Entries & Lines, Documents & Items, Allocations, Assets, Depreciation Runs, Card Packages, Stock Movements, Number Sequences, Audit Log, Idempotency Keys).
- [x] Immutable SQLite triggers (`BEFORE UPDATE/DELETE` abort on `journal_entries` and `journal_lines`).
- [x] Pure Business Posting Rules (`PostingRules`).
- [x] Unified Transaction Pipeline (`LedgerWriter`).
- [x] Mathematical Invariant Validator (`LedgerInvariants`).
- [x] Seed Data (Locked IFRS COA, Walk-in Cash Party, Currencies, Standard Treasuries).
- [x] Clean Dark/Light Theme with Arabic RTL support and Tabular Numerals.
- [x] Reusable Financial UI Components (`AmountText`, `MoneyField`, `StatCard`, `InvariantBanner`, etc.).
- [x] App Structure: Onboarding Flow + Developer/Inspector Screens (COA, Journal Browser, Trial Balance, Health Check).
- [x] Unit & Robolectric Integration Test Suite.

## Phase 2: Business Operations & Workflows (Completed)
- [x] Sales & Distribution Module:
  - Batch Card Voucher generation & tracking (Package details, wholesale/retail prices).
  - Grocery Agent delivery sheets & receivables management.
  - Direct Internet Subscription contracts & billing.
  - Quick POS cash sale workflow with atomic receipt and allocation.
  - Thermal receipt printing format & voucher preview.
- [x] Expense & Upstream ISP Management:
  - Direct upstream bandwidth payment vouchers (Starlink, Wholesale Fiber).
  - Fuel, generator maintenance, site rentals, technician salary slips.
  - Fixed Assets Register (Routers, Antennas, Towers) with straight-line monthly depreciation and retirement disposal.
- [x] Treasury & Multi-Currency Transfers:
  - Cash drawers, bank accounts, electronic wallets (Floosak, Kuraimi, Jawali, etc.).
  - Inter-treasury currency conversions with realized FX tracking (IAS 21).
- [x] Partner Equity & Profit Distribution:
  - Capital contributions, monthly drawings, automated dividend calculator.
- [x] Financial Statements & Reports (`ReportsScreen`):
  - Real-time Income Statement (P&L with direct service costing).
  - IFRS Balance Sheet with mathematical asset-liability equality verification.
  - Aging of Receivables (أعمار الديون) with 0-30, 31-60, 61-90, 90+ buckets.
  - Partner dividend distribution simulator.
  - Offline-first JSON backup export and restore verification.
- [x] Modern 2026 Navigation & UI Integration (`MainActivity`):
  - Bottom navigation bar across 7 core functional domains (Dashboard, Sales, Vouchers, Purchases & Assets, Parties & Stock, Reports, Audit).
  - Live IFRS invariant health badge in TopAppBar with direct inspection shortcut.
  - Reactive Snackbar notifications for business actions.
  - Full Arabic RTL support and tabular monospace numerals for financial figures.
- [x] Financial Reports Suite & Export Engine:
  - [x] Report 1: Monthly Profit Breakdown (12 months, ISP direct costing, Hare-Niemeyer partner shares).
  - [x] Report 2: Statement of Changes in Equity (IFRS 5-column movement matrix, in-kind contributions, partner balances).
  - [x] Report 3: Cash Flow Statement (IAS 7 Direct Method, compound invoice splits, zero-net transfer isolation).
  - [x] Report 4: Daily Cash Register (dual currency running balance, market valuation notes, dual reconciliation).
  - [x] Standard CSV Export (UTF-8 with BOM, standard period decimal formatting, zero Double/Float).
  - [x] Pixel-Perfect Arabic PDF Generation & Android Native Share via FileProvider.

## Phase 3: Hardware Integrations & Cloud Sync
- [ ] MikroTik RouterOS API / Hotspot Integration:
  - Sync active users, pull generated voucher batches, disable expired users.
- [ ] Bluetooth ESC/POS Thermal Printing for voucher slips and customer receipts.
- [ ] Offline-First Cloud Sync (Firebase Firestore / Cloud SQL backend) with conflict resolution.
- [ ] Role-Based Access Control (Owner, Accountant, Cashier, Auditor).
- [ ] Encrypted Automated Backups (Local storage & Google Drive).

- [ ] **Chunked Cloud Backup for Large Firestore Databases (النسخ السحابي المقسم للبيانات الكبيرة):**
  - Implement multi-document chunking and manifest indexing for backups exceeding the 900 KB guardrail (Firestore 1 MiB single document limit).
  - Split large collections (`journal_lines`, `documents`, `audit_log`) across sequential chunk documents (`users/{uid}/backup_chunks/part_N`) with a master manifest and aggregated SHA-256 verification.
- [ ] **Historical Partner Equity Ratio Table (سجل النسب التاريخية للشركاء):**
  - Implement dynamic time-slice versioning for partner equity ownership percentages (`partner_equity_ratios` table).
  - Support mid-year ownership transfers, capital restructurings, and new partner admissions with time-bounded validity periods (`effectiveFromEpochDay`, `effectiveToEpochDay`).
  - Audit trail of ownership percentage shifts without retroactively altering historical closed fiscal years.
- [ ] **Deep-Dive Audit Drill-Downs (التعمق الرقابي والتحليلي في بنود التقارير):**
  - Direct interactive click-through from any report matrix cell, monthly total, or activity line directly to the filtered journal lines browser (`JournalBrowserScreen`).
  - Source document inspector overlay showing original voucher, invoice lines, and electronic receipts.
  - Visual audit trail mapping Hare-Niemeyer compound invoice splits back to itemized hardware assets and service expenses.

