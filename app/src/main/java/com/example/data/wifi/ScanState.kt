package com.example.data.wifi

sealed interface ScanState {
    data class Ready(val accessPoints: List<WifiAccessPoint>) : ScanState
    object NoPermission : ScanState
    object WifiDisabled : ScanState
    object Empty : ScanState
    data class Error(val message: String) : ScanState
}
