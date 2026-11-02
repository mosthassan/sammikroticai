# Google Play Data Safety Declaration & Permissions Mapping
**Application Name:** SamMikrotik (سام ميكروتك)  
**Package:** `com.aistudio.sammikrotik.main` / `com.example`  
**Last Updated:** November 2026

---

## 1. Permissions Audit & Mapping

| Permission | Android API Purpose | App Feature / Usage | Leaves Device? | Notes / Security Controls |
| :--- | :--- | :--- | :---: | :--- |
| `android.permission.INTERNET` | Network communication | • Optional chunked cloud backup to Google Firebase Firestore.<br>• Optional AI Assistant queries via user API key.<br>• Google Sign-In authentication. | **Yes** (Only when initiated by user) | Encrypted in transit via HTTPS / TLS 1.3. Cloud backups are pre-encrypted client-side (AES-256-GCM). |
| `android.permission.ACCESS_NETWORK_STATE` | Check connectivity state | Pre-flight validation before initiating cloud backup or AI queries to give clear offline error messages. | **No** | Stays on device entirely. |
| `android.permission.ACCESS_WIFI_STATE` | Read Wi-Fi connection info | Query active Wi-Fi link speed, frequency, and connection parameters for network management. | **No** | Stays on device entirely. |
| `android.permission.CHANGE_WIFI_STATE` | Trigger Wi-Fi scan | Request Wi-Fi scan results from Android `WifiManager` for the spectrum analyzer screen. | **No** | Stays on device entirely. |
| `android.permission.ACCESS_FINE_LOCATION` | Precise hardware location | Required by the Android OS framework to deliver Wi-Fi access point scan results (BSSID, RSSI, channel). | **No** | **Never used for physical GPS tracking or geolocation.** Data is consumed in-memory only by the spectrum analyzer. |
| `android.permission.ACCESS_COARSE_LOCATION` | Approximate location | Fallback OS requirement alongside `ACCESS_FINE_LOCATION` for Wi-Fi scanning on older API levels. | **No** | **Never used for geolocation.** |
| `android.permission.NEARBY_WIFI_DEVICES` | Scan nearby Wi-Fi devices (Android 12+) | Allows scanning nearby Wi-Fi access points on Android 12+ (API 31+). | **No** | Explicitly configured with `android:usesPermissionFlags="neverForLocation"` to guarantee zero location derivation. |

> **Crucial Privacy Compliance Note:**  
> The application has **permanently eliminated** `android.permission.READ_CONTACTS`. Contact association for customers and suppliers strictly invokes Android's native `Intent.ACTION_PICK` system contact picker, granting one-time transient read permission exclusively to the user-selected contact URI.

---

## 2. Google Play Data Safety Form Copy-Paste Responses

### Section 1: Data Collection and Security
- **Does your app collect or share any of the required user data types?**  
  **Yes** (The app collects certain data types strictly for app functionality when initiated by the user).
- **Is all of the user data collected by your app encrypted in transit?**  
  **Yes** (All cloud requests use TLS/HTTPS, and cloud backups are encrypted client-side using AES-256-GCM before transmission).
- **Do you provide a way for users to request that their data be deleted?**  
  **Yes** (Users can delete all cloud backups within the app, and can wipe local data via app settings or Android App Storage).

---

### Section 2: Data Types Breakdown

#### A. Financial Info
- **Data Type:** Financial Info (Invoices, vouchers, journal entries, payment records, account balances).
- **Collected:** **Yes** (Saved in local SQLite database; optionally uploaded to private Firebase Firestore when user triggers cloud backup).
- **Shared:** **No** (Never shared with third parties or advertisers).
- **Purpose:** **App functionality** (Accounting, double-entry bookkeeping, financial reporting).
- **Ephemeral processing:** **No** (Stored persistently on device; stored encrypted in cloud if backup is triggered).
- **Is collection optional?** **Yes** (Cloud backup is optional; local storage is mandatory for app core functionality).

#### B. Personal Info (Name & Phone Number)
- **Data Type:** Personal Info (Customer / Supplier Names, Phone Numbers).
- **Collected:** **Yes** (Entered manually by user or chosen via Android system contact picker).
- **Shared:** **No** (Never transmitted to third parties).
- **Purpose:** **App functionality** (Associating invoices and receipts with specific clients or vendors).
- **Is collection optional?** **Yes** (Users can choose walk-in cash customer without entering names or phone numbers).

#### C. User Credentials & Account Info
- **Data Type:** User ID / Email address.
- **Collected:** **Yes** (Collected exclusively when the user logs into Google Sign-In for cloud backup authentication).
- **Shared:** **No** (Handled directly by Google Identity / Credential Manager).
- **Purpose:** **Account management & Cloud Backup ownership**.

#### D. Messages / Content (AI Assistant)
- **Data Type:** User-provided query text.
- **Collected:** **Yes** (Only when the user opens the AI Assistant and sends an explicit question).
- **Shared:** **Yes** (Sent directly to the user's chosen AI provider endpoint: Google Gemini, OpenAI, or DeepSeek, authenticated with the user's personal API key).
- **Purpose:** **App functionality** (Answering ISP operational queries).
- **Is collection optional?** **Yes** (The AI Assistant is entirely optional).

#### E. Location Data
- **Collected:** **No**.
- **Explanation:** Although Android requires `ACCESS_FINE_LOCATION` to access Wi-Fi scan results from `WifiManager`, the app **does not collect, store, or transmit GPS or physical location data**. On Android 12+, `NEARBY_WIFI_DEVICES` is declared with `neverForLocation`.

---

## 3. Advertising & Analytics Verification
- **Third-Party Advertising SDKs:** None (0).
- **Analytics / Tracking SDKs:** None (0).
- **Background Telemetry:** None (0).
