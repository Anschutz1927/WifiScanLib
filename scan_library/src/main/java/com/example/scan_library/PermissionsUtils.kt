package com.example.scan_library

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

fun requestScannerPermissions(activity: Activity, requestCode: Int) {
    val permissions = arrayOf(
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )
    ActivityCompat.requestPermissions(activity, permissions, requestCode)
}

fun checkScannerPermissions(context: Context): Boolean {
    val permissions = arrayOf(
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )
    for (permission in permissions) {
        if (!isPermissionGranted(context, permission)) {
            return false
        }
    }
    return true
}

private fun isPermissionGranted(context: Context, permission: String): Boolean {
    return ActivityCompat.checkSelfPermission(
        context, permission
    ) == PackageManager.PERMISSION_GRANTED
}