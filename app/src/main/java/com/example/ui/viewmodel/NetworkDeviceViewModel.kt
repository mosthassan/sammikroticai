package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.dao.DeviceDao
import com.example.data.network.DeviceSaveResult
import com.example.data.network.NetworkDevice
import com.example.data.network.NetworkRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

class NetworkDeviceViewModel(
    private val repository: NetworkRepository
) : ViewModel() {

    val devices: StateFlow<List<NetworkDevice>> = repository.devices

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    /**
     * Clones an existing device preserving all technical configurations
     * (Model, Device Type, Management Port, Subnet, Credentials, Location/Tower, and Notes)
     * while suffixing the Name with " - نسخة" and clearing the IP address.
     */
    fun cloneDevice(original: NetworkDevice, suffix: String = " - نسخة"): NetworkDevice {
        return original.copy(
            id = UUID.randomUUID().toString(),
            name = "${original.name.trim()}$suffix",
            ipAddress = "",
            macAddress = ""
        )
    }

    /**
     * Real-time check for duplicate IP on network devices.
     */
    suspend fun checkIpConflict(ip: String, currentDeviceId: String?): NetworkDevice? {
        val cleanIp = ip.trim()
        if (cleanIp.isBlank()) return null
        return repository.findDeviceByIp(cleanIp, excludeDeviceId = currentDeviceId)
    }

    suspend fun checkIpConflict(ip: String, currentDeviceId: Long): NetworkDevice? {
        val cleanIp = ip.trim()
        if (cleanIp.isBlank()) return null
        return repository.findDeviceByIp(cleanIp, currentDeviceId = currentDeviceId)
    }

    private val licenseManager: com.example.util.LicenseManager? = null

    fun saveDevice(device: NetworkDevice, onResult: (DeviceSaveResult) -> Unit = {}) {
        viewModelScope.launch {
            val isExisting = repository.devices.value.any { it.id == device.id }
            val lm = licenseManager ?: runCatching { com.example.util.LicenseManager.getInstance(com.example.SamMikrotikApp.instance) }.getOrNull()
            if (!isExisting && lm != null && !lm.canCreateDevice(repository.devices.value.size)) {
                _userMessage.emit(com.example.domain.subscription.SubscriptionLimits.LIMIT_EXCEEDED_MESSAGE_AR)
                onResult(DeviceSaveResult.LimitReached)
                return@launch
            }

            val result = repository.addOrUpdateDevice(device)
            when (result) {
                is DeviceSaveResult.Success -> {
                    _userMessage.emit("تم حفظ الجهاز [${device.name}] بنجاح")
                }
                is DeviceSaveResult.IpConflict -> {
                    _userMessage.emit("تعارض: عنوان الـ IP مستخدم مسبقاً بواسطة [${result.conflictingDevice.name}]")
                }
                is DeviceSaveResult.LimitReached -> {
                    _userMessage.emit(com.example.domain.subscription.SubscriptionLimits.LIMIT_EXCEEDED_MESSAGE_AR)
                }
            }
            onResult(result)
        }
    }

    fun deleteDevice(deviceId: String) {
        viewModelScope.launch {
            repository.deleteDevice(deviceId)
            _userMessage.emit("تم حذف الجهاز بنجاح")
        }
    }
}
