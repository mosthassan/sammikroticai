package com.example.domain.subscription

/**
 * Security policy for identifying SamMikrotik System Administrators.
 * Grants super-user privileges, unlimited PRO status, and in-app Admin Dashboard access.
 */
object AdminAuthPolicy {

    // Primary administrator emails
    private val ADMIN_EMAILS = setOf(
        "mosthassan.ye@gmail.com",
        "mosthassan.ye4@gmail.com"
    )

    /**
     * Checks if the given email has administrative privileges.
     * Supports exact match and normalized prefix matching.
     */
    fun isAdmin(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        val clean = email.trim().lowercase()
        return ADMIN_EMAILS.contains(clean) || clean.startsWith("mosthassan.ye")
    }
}
