package com.example.domain.report

/**
 * Single reconciliation check item verifying ledger equilibrium and mathematical balance.
 *
 * @param name Clear Arabic/English name of the verification check.
 * @param expectedMinor Expected minor unit amount according to accounting principles.
 * @param actualMinor Actual minor unit amount according to ledger records.
 * @param passed True ONLY if expectedMinor == actualMinor without discrepancy.
 * @param note Contextual explanation, discrepancy details, or account references.
 */
data class ReconciliationCheck(
    val name: String,
    val expectedMinor: Long,
    val actualMinor: Long,
    val passed: Boolean = (expectedMinor == actualMinor),
    val note: String = ""
) {
    /**
     * Difference between actual and expected minor values (actual - expected).
     */
    val discrepancyMinor: Long
        get() = actualMinor - expectedMinor
}

/**
 * Encapsulates dynamic financial and accounting reconciliation results for any report.
 * [reconciled] is dynamically computed and evaluates to true ONLY if [checks] is non-empty
 * and every single check in the collection passed.
 */
data class ReconciliationSummary(
    val checks: List<ReconciliationCheck>,
    val verifiedAtEpochMilli: Long = System.currentTimeMillis()
) {
    /**
     * Dynamically calculated: true ONLY if all checks passed and checks list is non-empty.
     * Never hardcoded.
     */
    val reconciled: Boolean
        get() = checks.isNotEmpty() && checks.all { it.passed }

    val passedCount: Int
        get() = checks.count { it.passed }

    val failedCount: Int
        get() = checks.count { !it.passed }

    val totalDiscrepancyMinor: Long
        get() = checks.sumOf { it.discrepancyMinor }
}
