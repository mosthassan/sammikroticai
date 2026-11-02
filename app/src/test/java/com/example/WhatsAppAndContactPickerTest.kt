package com.example

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.test.core.app.ApplicationProvider
import com.example.core.model.UuidUtils
import com.example.data.local.AppDatabase
import com.example.data.local.entity.PartyEntity
import com.example.data.repository.AccountingRepository
import com.example.ui.viewmodel.PartyViewModel
import com.example.util.WhatsAppDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WhatsAppAndContactPickerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testPhoneNumberSanitizationAndIntelligentNormalization() {
        // 9-digit local Yemeni number starting with 7
        assertEquals("+967771234567", WhatsAppDispatcher.sanitizePhoneNumber("771234567"))
        assertEquals("+967731234567", WhatsAppDispatcher.sanitizePhoneNumber("731234567"))
        assertEquals("+967711234567", WhatsAppDispatcher.sanitizePhoneNumber("711234567"))
        assertEquals("+967701234567", WhatsAppDispatcher.sanitizePhoneNumber("701234567"))

        // With punctuation, spaces, and brackets
        assertEquals("+967771234567", WhatsAppDispatcher.sanitizePhoneNumber("771 234 567"))
        assertEquals("+967771234567", WhatsAppDispatcher.sanitizePhoneNumber("771-234-567"))
        assertEquals("+967771234567", WhatsAppDispatcher.sanitizePhoneNumber("(771) 234-567"))

        // 10-digit with leading 0 (07xx xxx xxx)
        assertEquals("+967771234567", WhatsAppDispatcher.sanitizePhoneNumber("0771234567"))
        assertEquals("+967771234567", WhatsAppDispatcher.sanitizePhoneNumber("07-71234567"))
        assertEquals("+967771234567", WhatsAppDispatcher.sanitizePhoneNumber("(07) 71 234 567"))

        // 12-digit already with 967
        assertEquals("+967771234567", WhatsAppDispatcher.sanitizePhoneNumber("967771234567"))
        assertEquals("+967771234567", WhatsAppDispatcher.sanitizePhoneNumber("+967771234567"))
        assertEquals("+967771234567", WhatsAppDispatcher.sanitizePhoneNumber("+967 77-123-4567"))

        // 14-digit with 00967
        assertEquals("+967771234567", WhatsAppDispatcher.sanitizePhoneNumber("00967771234567"))
        assertEquals("+967771234567", WhatsAppDispatcher.sanitizePhoneNumber("00967 771 234 567"))

        // International numbers with +
        assertEquals("+966501234567", WhatsAppDispatcher.sanitizePhoneNumber("+966 50 123 4567"))

        // Empty string
        assertEquals("", WhatsAppDispatcher.sanitizePhoneNumber(""))
        assertEquals("", WhatsAppDispatcher.sanitizePhoneNumber("   "))
    }

    @Test
    fun testGetCleanDigitsForWhatsApp() {
        assertEquals("967771234567", WhatsAppDispatcher.getCleanDigitsForWhatsApp("+967771234567"))
        assertEquals("967771234567", WhatsAppDispatcher.getCleanDigitsForWhatsApp("771234567"))
        assertEquals("967771234567", WhatsAppDispatcher.getCleanDigitsForWhatsApp("0771234567"))
        assertEquals("966501234567", WhatsAppDispatcher.getCleanDigitsForWhatsApp("+966501234567"))
        assertEquals("", WhatsAppDispatcher.getCleanDigitsForWhatsApp(""))
    }

    @Test
    fun testSendTextMessageDispatchesIntent() {
        val phone = "771234567"
        val message = "مرحباً بك في شبكة SamMikrotik"

        WhatsAppDispatcher.sendTextMessage(context, phone, message)

        val shadowApp = Shadows.shadowOf(context as android.app.Application)
        val startedIntent = shadowApp.nextStartedActivity
        assertNotNull("An intent should have been dispatched", startedIntent)
        assertEquals(Intent.ACTION_VIEW, startedIntent.action)
        val dataUri = startedIntent.data
        assertNotNull(dataUri)
        assertTrue("URI should target wa.me", dataUri.toString().contains("wa.me/967771234567"))
        assertTrue("URI should contain encoded message", dataUri.toString().contains("text="))
    }

    @Test
    fun testDispatchInvoiceReceiptGeneratesCompleteMessage() {
        val party = PartyEntity(
            id = UuidUtils.newTimeOrderedId(),
            name = "بقالة النصر",
            phone = "+967771234567",
            isCustomer = true
        )

        WhatsAppDispatcher.dispatchInvoiceReceipt(
            context = context,
            party = party,
            docNumber = 1001L,
            totalText = "50,000 ر.ي",
            paidText = "20,000 ر.ي",
            remainingText = "30,000 ر.ي",
            itemsSummary = "50 كرت فئة 1000",
            extraNotes = "تسليم للمندوب"
        )

        val shadowApp = Shadows.shadowOf(context as android.app.Application)
        val startedIntent = shadowApp.nextStartedActivity
        assertNotNull(startedIntent)
        val uriStr = startedIntent.data.toString()
        assertTrue("URI should target customer phone", uriStr.contains("wa.me/967771234567"))
        assertTrue("Should mention invoice #1001", uriStr.contains("1001"))
    }

    @Test
    fun testDispatchStatementSummaryGeneratesCompleteMessage() {
        val party = PartyEntity(
            id = UuidUtils.newTimeOrderedId(),
            name = "بقالة الأمل",
            phone = "777888999",
            isCustomer = true
        )

        WhatsAppDispatcher.dispatchStatementSummary(
            context = context,
            party = party,
            openingBalanceText = "10,000 ر.ي",
            closingBalanceText = "45,000 ر.ي",
            transactionCount = 12
        )

        val shadowApp = Shadows.shadowOf(context as android.app.Application)
        val startedIntent = shadowApp.nextStartedActivity
        assertNotNull(startedIntent)
        val uriStr = startedIntent.data.toString()
        assertTrue("URI should target normalized phone", uriStr.contains("wa.me/967777888999"))
    }

    @Test
    fun testPartyInsertAndUpdateViaRepository() = runBlocking {
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = AccountingRepository(db)

        val partyId = UuidUtils.newTimeOrderedId()
        val initialParty = PartyEntity(
            id = partyId,
            name = "مركز التوزيع السريع",
            phone = "+967771234567",
            isCustomer = true,
            creditLimitMinor = 5000000L
        )

        repo.insertParty(initialParty)

        val loaded = db.partyDao().getPartyById(partyId)
        assertNotNull(loaded)
        assertEquals("مركز التوزيع السريع", loaded?.name)
        assertEquals("+967771234567", loaded?.phone)

        // Update phone and credit limit
        val updatedParty = initialParty.copy(
            phone = "+967779999999",
            creditLimitMinor = 10000000L
        )
        repo.updateParty(updatedParty)

        val reloaded = db.partyDao().getPartyById(partyId)
        assertNotNull(reloaded)
        assertEquals("+967779999999", reloaded?.phone)
        assertEquals(10000000L, reloaded?.creditLimitMinor)

        db.close()
    }

    @Test
    fun testAndroidManifestDoesNotDeclareReadContactsPermission() {
        val manifestFile = File("src/main/AndroidManifest.xml").takeIf { it.exists() }
            ?: File("app/src/main/AndroidManifest.xml").takeIf { it.exists() }
            ?: File("/app/applet/app/src/main/AndroidManifest.xml").takeIf { it.exists() }
            ?: File("/app/src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml should exist", manifestFile.exists())
        val content = manifestFile.readText()
        assertFalse(
            "AndroidManifest.xml must not declare READ_CONTACTS permission",
            content.contains("android.permission.READ_CONTACTS")
        )

        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            android.content.pm.PackageManager.GET_PERMISSIONS
        )
        val permissions = packageInfo.requestedPermissions ?: emptyArray()
        assertFalse(
            "Package requested permissions must not contain READ_CONTACTS",
            permissions.contains("android.permission.READ_CONTACTS")
        )
    }

    @Test
    fun testResolveContactInfoResolvesWithoutRuntimePermissions() {
        Robolectric.buildContentProvider(MockContactsProvider::class.java)
            .create("com.android.contacts")
            .get()

        val contactUri = Uri.parse("content://com.android.contacts/data/123")
        val contactInfo = PartyViewModel.resolveContactInfo(context, contactUri)

        assertNotNull("ContactInfo should be resolved successfully", contactInfo)
        assertEquals("سالم محمد", contactInfo?.name)
        assertEquals("+967771234567", contactInfo?.phone)
    }

    class MockContactsProvider : ContentProvider() {
        override fun onCreate(): Boolean = true
        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?
        ): Cursor {
            val cursor = MatrixCursor(
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                )
            )
            cursor.addRow(arrayOf("771234567", "سالم محمد"))
            return cursor
        }
        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
    }
}
