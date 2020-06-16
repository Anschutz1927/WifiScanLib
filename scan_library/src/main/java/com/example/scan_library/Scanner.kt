package com.example.scan_library

import android.content.*
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

class Scanner private constructor(
    private val context: Context,
    private val callback: OnWifiScanCallback
) : LifecycleObserver {

    private val wifiManager: WifiManager =
        context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val wifiActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when {
                intent == null -> return
                intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION ->
                    onReceiveScanResult(intent)
                intent.action == WifiManager.WIFI_STATE_CHANGED_ACTION ->
                    onReceiveStateChange()
            }
        }
    }
    private var state = State.NONE

    constructor(
        context: Context,
        lifecycle: Lifecycle,
        callback: OnWifiScanCallback
    ) : this(context, callback) {
        lifecycle.addObserver(this)
    }

    /**
     * Before call [Scanner.performWifiScan] you have to grant location permissions and enable geo services
     * otherwise [OnWifiScanCallback.onUnsuccessful] will be called
     */
    fun performWifiScan() {
        when {
            !checkScannerPermissions(context) -> callback.onUnsuccessful(Exception("Location permissions needed"))
            wifiManager.wifiState == WifiManager.WIFI_STATE_ENABLING -> return
            wifiManager.isWifiEnabled -> {
                startScan()
            }
            isVersionAboveP() -> {
                context.startActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
                callback.onUnsuccessful(Exception("Enabled wifi required"))
            }
            else -> {
                val filter = IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION)
                context.registerReceiver(wifiActionReceiver, filter)
                if (wifiManager.setWifiEnabled(true)) {
                    state = State.ENABLING
                } else {
                    callback.onUnsuccessful(Exception("Error while enabling wifi"))
                    context.unregisterReceiver(wifiActionReceiver)
                }
            }
        }
    }

    private fun startScan() {
        val prefs = context.getSharedPreferences(Scanner::class.simpleName, Context.MODE_PRIVATE)
        if (isAllowedScan(prefs)) {
            registerReceiver()
            if (wifiManager.startScan()) {
                state = State.SCANNING
                prefs.edit().putLong(LAST_SCAN_KEY, System.currentTimeMillis()).apply()
            } else {
                callback.onUnsuccessful(Exception("Scan is not started. Is location service enabled?"))
                unregisterReceiver()
            }
        } else {
            callback.onUnsuccessful(Exception("Timeout exception"))
        }
    }

    private fun onReceiveScanResult(intent: Intent) {
        if (intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)) {
            val results = wifiManager.scanResults
            callback.onSuccess(results)
        } else {
            callback.onUnsuccessful(Exception("Scan result returned false"))
        }
        unregisterReceiver()
    }

    private fun onReceiveStateChange() {
        println(wifiManager.wifiState)
        if (wifiManager.wifiState == WifiManager.WIFI_STATE_ENABLED && state == State.ENABLING) {
            unregisterReceiver()
            startScan()
        }
    }

    private fun isAllowedScan(prefs: SharedPreferences): Boolean {
        val lastScan = prefs.getLong(LAST_SCAN_KEY, 0)
        return state == State.NONE && System.currentTimeMillis() - lastScan > SCAN_DELAY
    }

    private fun isVersionAboveP(): Boolean = Build.VERSION.SDK_INT > Build.VERSION_CODES.P

    private fun registerReceiver() {
        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(wifiActionReceiver, filter)
    }

    private fun unregisterReceiver() {
        if (state != State.NONE) {
            context.unregisterReceiver(wifiActionReceiver)
            state = State.NONE
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onDestroy() {
        unregisterReceiver()
    }
}

private enum class State { NONE, ENABLING, SCANNING }

interface OnWifiScanCallback {
    fun onSuccess(data: List<ScanResult>)
    fun onUnsuccessful(e: Exception)
}