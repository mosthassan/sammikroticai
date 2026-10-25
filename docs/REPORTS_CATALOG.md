# SamMikrotik - Financial Reports Catalog (فهرس التقارير المالية القياسية)

This document provides complete accounting specifications, data sources, mathematical formulas, and reconciliation invariants for the 4 core financial reports in SamMikrotik.

---

## Report 1: Monthly Profit & Loss Statement (قائمة الأرباح والخسائر الشهرية - 12 شهراً)

### 1. Purpose
Provides a multi-period (12-month) operational performance and profitability breakdown for Internet Service Providers (ISPs). It isolates direct upstream internet capacity costs (Starlink, wholesale fiber) from operating expenses, calculates gross and net margins in basis points, and dynamically models partner profit shares.

### 2. Accounting Formula
$$\text{Net Revenue} = \text{Card Sales (4101)} + \text{ISP Subscriptions (4201)} + \text{Other Revenue} - \text{Sales Returns (4102)}$$
$$\text{Gross Profit} = \text{Net Revenue} - \text{Direct ISP Bandwidth Cost (5101)}$$
$$\text{Gross Margin (bp)} = \left(\frac{\text{Gross Profit}}{\text{Net Revenue}}\right) \times 10,000$$
$$\text{Total Operating Expenses} = 5201 (\text{Diesel/Electricity}) + 5202 (\text{Spares}) + 5204 (\text{Salaries}) + 5299 (\text{Misc})$$
$$\text{Realized Operating Profit} = \text{Gross Profit} - \text{Total Operating Expenses} - \text{Depreciation (5203)} + \text{Realized FX Net (4901 - 5901)}$$
$$\text{Net Profit} = \text{Realized Operating Profit} + \text{Unrealized FX Gain/Loss}$$
$$\text{Cumulative Profit}_m = \sum_{k=1}^{m} \text{Net Profit}_k$$
$$\text{Partner Share}_i = \text{HareNiemeyer}(\text{Net Profit}, \text{EquityPercentage}_i)$$

### 3. Data Source
- Aggregated from `journal_lines` grouped by fiscal periods ($1..12$) joined with `chart_of_accounts`.
- Active partners and equity modes queried from `parties` where `isPartner = 1` and `organizations`.

### 4. Reconciliation Checks
- **Gross Profit Integrity:** $\text{Gross Profit} == \text{Net Revenue} - \text{Direct ISP Cost}$.
- **Operating Margin Integrity:** $\text{Net Profit} == \text{Gross Profit} - \text{Expenses} - \text{Depreciation} \pm \text{FX}$.
- **Hare-Niemeyer Profit Invariant:** $\sum_{i} \text{Partner Share}_i == \text{Net Profit}$ down to a single minor unit (1 Rial).
- **Balance Sheet Tie-in:** $\text{Cumulative Year Profit} == \text{Balance Sheet Unclosed Net Profit}$.

---

## Report 2: Statement of Changes in Equity (قائمة التغيرات في حقوق الملكية - IFRS)

### 1. Purpose
Implements a 5-column equity movement matrix complying with IFRS / IAS 1. It details the progression of Share Capital (`3101`), Partner Current Accounts (`3201`), Retained Earnings (`3301`), and Period Profit (`3302`), detailing capital injections, in-kind hardware contributions, drawings, and distributions.

### 2. Accounting Formula
$$\text{Total Opening Equity} = \text{Capital (3101)} + \text{Current (3201)} + \text{Retained Earnings (3301)}$$
$$\text{Period Movement} = \text{Contributions (Cash + In-Kind)} + \text{Period Profit} - \text{Drawings} - \text{Dividends} \pm \text{Settlements}$$
$$\text{Total Closing Equity} = \text{Beginning Equity} + \text{Period Movement}$$
$$\text{Partner Individual Equity}_i = \text{Closing Capital}_i + \text{Closing Current}_i + \text{Estimated Profit Share}_i$$

### 3. Data Source
- `journal_lines` on accounts `3101`, `3201`, `3301`, `3302`.
- Classifies motion types: `CASH_CONTRIBUTION`, `IN_KIND_CONTRIBUTION`, `WITHDRAWAL`, `PAID_ON_BEHALF`, `SETTLEMENT`.
- In-kind contributions link to asset registrations in `assets` and `document_items`.

### 4. Reconciliation Checks
- **Ledger Balance Tie-in:** $\text{Total Matrix Closing Equity} == \text{GL Sum of Account Balances (3101 + 3201 + 3301 + 3302)}$.
- **Individual Partner Sum Tie-in:** $\sum_i \text{Partner Closing Equity}_i == \text{Total Matrix Closing Equity}$.
- **Mathematical Continuity:** $\text{Beginning Balance} + \sum \text{Movements} == \text{Ending Balance}$.

---

## Report 3: Cash Flow Statement (قائمة التدفقات النقدية - IAS 7 Direct Method)

### 1. Purpose
Classifies all cash flows under the Direct Method as prescribed by IAS 7 into Operating, Investing, and Financing activities. Accurately handles multi-item compound invoices via the Hare-Niemeyer largest-remainder algorithm, excludes zero-net internal transfers, and isolates foreign exchange cash revaluations.

### 2. Accounting Formula
$$\text{Net Operating Cash Flow} = \sum \text{Operating Inflows} - \sum \text{Operating Outflows}$$
$$\text{Net Investing Cash Flow} = \sum \text{Asset Disposals} - \sum \text{Capital Expenditures (Direct + Split)}$$
$$\text{Net Financing Cash Flow} = \sum \text{Partner Equity Deposits} - \sum \text{Partner Drawings} - \sum \text{Dividends Paid}$$
$$\text{Net Change in Cash} = \text{Operating} + \text{Investing} + \text{Financing} + \text{FX Impact on Cash}$$
$$\text{Closing Cash} = \text{Opening Cash} + \text{Net Change in Cash}$$

### 3. Data Source
- `journal_lines` for cash accounts `1101` (Vaults) and `1102` (Banks & Wallets).
- Offset scanning against contra-lines in each journal entry.
- Compound invoice splits driven by `HareNiemeyerSplitter` based on itemized `document_items`.

### 4. Reconciliation Checks
- **Cash Continuity Invariant:** $\text{Opening Cash} + \text{Net Change in Cash} == \text{Closing Cash}$.
- **Ledger Equality:** $\text{Report Closing Cash} == \text{Vault Cash (1101)} + \text{Bank Cash (1102)}$.
- **Hare-Niemeyer Split Invariant:** $\text{Allocated Operating} + \text{Allocated Investing} == \text{Total Cash Paid}$.
- **Zero-Net Transfer Exclusion:** All inter-treasury transfers and currency exchanges sum to zero net cash impact.

---

## Report 4: Daily Cash Register (سجل الصندوق اليومي)

### 1. Purpose
Provides a detailed chronological ledger for a specific physical treasury box or electronic wallet and currency. Tracks incoming receipts, outgoing payments, and cumulative running balances in both original currency and functional currency (YER), with informative market valuation notes.

### 2. Accounting Formula
$$\text{Running Balance}_{\text{Orig}, t} = \text{Running Balance}_{\text{Orig}, t-1} + \text{Inflow}_{\text{Orig}, t} - \text{Outflow}_{\text{Orig}, t}$$
$$\text{Running Balance}_{\text{Base}, t} = \text{Running Balance}_{\text{Base}, t-1} + \text{Inflow}_{\text{Base}, t} - \text{Outflow}_{\text{Base}, t}$$
$$\text{Market Value Estimate (YER)} = \frac{\text{Running Balance}_{\text{Orig}} \times \text{CurrentRateMicros}}{1,000,000}$$
$$\text{Unrealized Delta (YER)} = \text{Market Value Estimate} - \text{Book Value (Base YER)}$$

*Note text convention:* `"تقدير بسعر اليوم، لا يدخل في أي قيد"` (Informational estimation only, zero ledger impact).

### 3. Data Source
- `journal_lines` filtered by `treasuryId` and currency, chronologically ordered by `dateEpochDay`, `entryNumber`, and `lineId`.
- Joined with `journal_entries`, `documents`, and `parties`.
- Opening balances calculated from all lines with `dateEpochDay < startDateEpochDay`.

### 4. Reconciliation Checks
- **Original Currency Reconciliation:** $\text{Closing Balance (Original Currency)} == \text{Treasury Record Balance (treasury_accounts.balanceMinor)}$.
- **Base Currency Reconciliation:** $\text{Closing Balance (Base Currency)} == \text{General Ledger Balance (chart_of_accounts 1101/1102)}$.
- **Historical Continuity:** $\text{Opening Balance} + \sum \text{Inflows} - \sum \text{Outflows} == \text{Closing Balance}$.

---

## Export Standards (معايير التصدير والمشاركة)

All 4 reports adhere to the following unified export standards:
1. **UTF-8 with BOM CSV:** Includes `\uFEFF` byte order mark for immediate compatibility with Microsoft Excel and Google Sheets.
2. **Zero-Double/Float Decimal Formatting:** Decimal values use period `.` separators without thousands commas (e.g., `1234.50`) formatted strictly with integer math.
3. **Arabic Pixel-Perfect PDF:** Rendered on A4 canvases with StaticLayout RTL text, cyber-fintech styling (Dark Navy & MikroTik Cyan), alternating rows, KPI cards, and dynamic reconciliation badges.
4. **Android Native Share:** Dispatched securely using `FileProvider` (`${applicationId}.fileprovider`) to any compatible app (WhatsApp, Google Drive, Email, External PDF viewer).
