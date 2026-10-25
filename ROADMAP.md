# SamMikrotik - Product & Engineering Roadmap

Please refer to the full specification and details in [docs/ROADMAP.md](docs/ROADMAP.md).

## Phase 1: Accounting Core & Architecture (Completed)
- [x] Zero-Float Money Engine (`Money`, `CurrencyCode`, `ExchangeRate`, integer micro-rates).
- [x] Room Database v1 Schema with immutable SQLite triggers.
- [x] Pure Business Posting Rules & Unified Transaction Pipeline (`LedgerWriter`).
- [x] Mathematical Invariant Validator (`LedgerInvariants`).

## Phase 2: Business Operations & Financial Reports (Completed)
- [x] Sales, Inventory, Fixed Assets, Vouchers & Upstream ISP Contracts.
- [x] Financial Reports Suite:
  - [x] Report 1: Monthly Profit Breakdown (12 months, ISP direct costing, Hare-Niemeyer partner shares).
  - [x] Report 2: Statement of Changes in Equity (IFRS 5-column movement matrix, in-kind contributions, partner balances).
  - [x] Report 3: Cash Flow Statement (IAS 7 Direct Method, compound invoice splits, zero-net transfer isolation).
  - [x] Report 4: Daily Cash Register (dual currency running balance, market valuation notes, dual reconciliation).
  - [x] Standard CSV Export (UTF-8 with BOM, standard period decimal formatting, zero Double/Float).
  - [x] Pixel-Perfect Arabic PDF Generation & Android Native Share via FileProvider.

## Phase 3: Hardware Integrations & Cloud Sync
- [ ] MikroTik RouterOS API / Hotspot Integration.
- [ ] Bluetooth ESC/POS Thermal Printing for voucher slips and customer receipts.
- [ ] Offline-First Cloud Sync (Firebase Firestore / Cloud SQL backend).
- [ ] Role-Based Access Control (Owner, Accountant, Cashier, Auditor).

## Future Enhancements & Advanced Audit Roadmap
- [ ] **Historical Partner Equity Ratio Table (سجل النسب التاريخية للشركاء):**
  - Implement dynamic time-slice versioning for partner equity ownership percentages (`partner_equity_ratios` table).
  - Support mid-year ownership transfers, capital restructurings, and new partner admissions with time-bounded validity periods (`effectiveFromEpochDay`, `effectiveToEpochDay`).
  - Audit trail of ownership percentage shifts without retroactively altering historical closed fiscal years.
- [ ] **Deep-Dive Audit Drill-Downs (التعمق الرقابي والتحليلي في بنود التقارير):**
  - Direct interactive click-through from any report matrix cell, monthly total, or activity line directly to the filtered journal lines browser (`JournalBrowserScreen`).
  - Source document inspector overlay showing original voucher, invoice lines, and electronic receipts.
  - Visual audit trail mapping Hare-Niemeyer compound invoice splits back to itemized hardware assets and service expenses.
