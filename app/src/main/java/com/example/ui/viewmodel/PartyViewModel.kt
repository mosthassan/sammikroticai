package com.example.ui.viewmodel

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.PartyEntity
import com.example.data.repository.AccountingRepository
import com.example.util.WhatsAppDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ContactInfo(
    val name: String,
    val phone: String
)

class PartyViewModel(
    private val repository: AccountingRepository
) : ViewModel() {

    val allParties: StateFlow<List<PartyEntity>> = repository.allParties.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private val _selectedParty = MutableStateFlow<PartyEntity?>(null)
    val selectedParty: StateFlow<PartyEntity?> = _selectedParty.asStateFlow()

    fun selectParty(party: PartyEntity?) {
        _selectedParty.value = party
    }

    fun insertParty(party: PartyEntity, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.insertParty(party)
                _userMessage.emit("تمت إضافة الطرف بنجاح")
                onSuccess()
            } catch (e: Exception) {
                _userMessage.emit("فشل إضافة الطرف: ${e.message}")
            }
        }
    }

    fun updateParty(party: PartyEntity, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.updateParty(party)
                _userMessage.emit("تم تحديث بيانات الطرف بنجاح")
                onSuccess()
            } catch (e: Exception) {
                _userMessage.emit("فشل تحديث بيانات الطرف: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "PartyContactPicker"

        /**
         * Resolves contact name and phone number directly from the single CommonDataKinds.Phone record Uri
         * returned by the contact picker (Intent.ACTION_PICK with CommonDataKinds.Phone.CONTENT_URI).
         * Utilizes the transient read permission granted by the Android OS picker without broad READ_CONTACTS.
         */
        fun resolveContactInfo(context: Context, contactUri: Uri): ContactInfo? {
            var displayName = ""
            var rawPhoneNumber = ""

            try {
                context.contentResolver.query(
                    contactUri,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        if (numIdx != -1) rawPhoneNumber = cursor.getString(numIdx).orEmpty()
                        if (nameIdx != -1) displayName = cursor.getString(nameIdx).orEmpty()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error resolving contact from picker uri: ${e.message}", e)
                return null
            }

            val sanitizedPhone = WhatsAppDispatcher.sanitizePhoneNumber(rawPhoneNumber)
            return ContactInfo(
                name = displayName.trim(),
                phone = sanitizedPhone
            )
        }

        /**
         * Backward compatibility alias for resolveContactInfo.
         */
        fun extractContact(context: Context, contactUri: Uri): ContactInfo? = resolveContactInfo(context, contactUri)
    }
}
