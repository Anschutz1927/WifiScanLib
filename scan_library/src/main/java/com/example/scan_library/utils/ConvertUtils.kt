package com.example.scan_library.utils

import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import com.example.scan_library.ScannerResult

fun ScanResult.toScannerResult(): ScannerResult = ScannerResult(
    SSID, WifiManager.calculateSignalLevel(level, 100),
    capabilities.replace("[ESS]", "").replace("[WPS]", "").isNotEmpty()
)