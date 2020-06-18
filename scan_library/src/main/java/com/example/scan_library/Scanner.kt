package com.example.scan_library

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.example.scan_library.utils.LAST_SCAN_KEY
import com.example.scan_library.utils.SCAN_DELAY
import com.example.scan_library.utils.checkScannerPermissions
import com.example.scan_library.utils.toScannerResult
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

class Scanner(
    private val context: Context,
    lifecycle: Lifecycle? = null,
    private val callback: OnWifiScanCallback
) : LifecycleObserver {

    private val wifiManager: WifiManager =
        context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val prefs =
        context.getSharedPreferences(Scanner::class.simpleName, Context.MODE_PRIVATE)
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
    private var lastSuccessResult = listOf<ScannerResult>()
    private var disposable: Disposable? = null

    /**
     * [updateDelay] must be larger than 30L
     */
    var updateDelay = 30L
        set(value) {
            field = when {
                value < 30L -> 30L
                else -> value
            }
        }

    init {
        lifecycle?.addObserver(this)
    }

    fun getLastSuccessResult(): List<ScannerResult> {
        return lastSuccessResult
    }

    /**
     * Before call [Scanner.performWifiScan] you have to grant location permissions and enable geo services
     * otherwise [OnWifiScanCallback.onUnsuccessful] will be called
     */
    fun performWifiScan() {
        if (!checkScannerPermissions(context)) {
            sendError(Error.LOCATION_PERMISSION_REQUIRED)
        } else if (!isAllowedByState() || !isAllowedByTimeout()) {
            sendError(Error.TIMEOUT)
        } else if (isWifiStateReady()) {
            startScan()
        } else if (isWifiStateDisabled()) {
            enableWifiAndScan(State.ENABLING_AND_SINGLE_UPDATE)
        } else {
            sendError(Error.WIFI_NOT_READY)
        }
    }

    /**
     * Before call [Scanner.performWifiScanWithUpdates] you have to grant location permissions and enable geo services
     * otherwise [OnWifiScanCallback.onUnsuccessful] will be called
     */
    fun performWifiScanWithUpdates() {
        if (!checkScannerPermissions(context)) {
            sendError(Error.LOCATION_PERMISSION_REQUIRED)
        } else if (!isAllowedByState()) {
            sendError(Error.ALREADY_RUN)
        } else if (isWifiStateReady()) {
            startScanWithUpdates()
        } else if (isWifiStateDisabled()) {
            enableWifiAndScan(State.ENABLING_AND_MULTI_UPDATE)
        } else {
            sendError(Error.WIFI_NOT_READY)
        }
    }

    private fun startScan() {
        registerReceiver(IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        state = State.SCANNING
        if (wifiManager.startScan()) {
            prefs.edit().putLong(LAST_SCAN_KEY, System.currentTimeMillis()).apply()
        } else {
            sendError(Error.SCAN_FAILED)
            unregisterReceiver()
        }
    }

    private fun startScanWithUpdates() {
        var startDelay = 0L
        if (!isAllowedByTimeout()) {
            callback.onSuccess(lastSuccessResult)
            startDelay = 30L
        }
        registerReceiver(IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        state = State.UPDATED_SCANNING
        disposable = Flowable.interval(startDelay, updateDelay, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread()).map {
                if (state == State.UPDATED_SCANNING && isWifiStateReady() && wifiManager.startScan()) {
                    prefs.edit().putLong(LAST_SCAN_KEY, System.currentTimeMillis()).apply()
                    return@map true
                }
                return@map false
            }.takeUntil { return@takeUntil !it }
            .subscribe(
                { result -> println("Scan start result = $result") },
                {
                    it.printStackTrace()
                    sendError(Error.SCAN_FAILED)
                }
            ) {
                terminateScan()
                sendError(Error.RESULTS_FAILED)
            }
    }

    private fun enableWifiAndScan(enableWithState: State) {
        if (isVersionAboveP()) {
            context.startActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
            sendError(Error.ENABLED_WIFI_REQUIRED)
        } else {
            registerReceiver(IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION))
            state = enableWithState
            if (!wifiManager.setWifiEnabled(true)) {
                sendError(Error.WIFI_ENABLING_ERROR)
                unregisterReceiver()
            }
        }
    }

    private fun onReceiveScanResult(intent: Intent) {
        if (intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)) {
            lastSuccessResult = wifiManager.scanResults.parallelStream()
                .map { return@map it.toScannerResult() }.collect(Collectors.toList())
            callback.onSuccess(lastSuccessResult)
            if (state != State.UPDATED_SCANNING) {
                unregisterReceiver()
            }
        } else {
            sendError(Error.RESULTS_FAILED)
            terminateScan()
        }
    }

    private fun onReceiveStateChange() {
        if (isWifiStateReady()) {
            if (state == State.ENABLING_AND_SINGLE_UPDATE) {
                unregisterReceiver()
                startScan()
            } else if (state == State.ENABLING_AND_MULTI_UPDATE) {
                unregisterReceiver()
                startScanWithUpdates()
            }
        }
    }

    private fun isAllowedByState(): Boolean = state == State.NONE

    private fun isAllowedByTimeout(): Boolean {
        val lastScan = prefs.getLong(LAST_SCAN_KEY, 0)
        return System.currentTimeMillis() - lastScan > SCAN_DELAY
    }

    private fun isWifiStateReady(): Boolean =
        wifiManager.wifiState == WifiManager.WIFI_STATE_ENABLED

    private fun isWifiStateDisabled(): Boolean =
        wifiManager.wifiState == WifiManager.WIFI_STATE_DISABLED

    private fun isVersionAboveP(): Boolean = Build.VERSION.SDK_INT > Build.VERSION_CODES.P

    private fun sendError(e: Error) = callback.onUnsuccessful(e)

    private fun registerReceiver(filter: IntentFilter) {
        context.registerReceiver(wifiActionReceiver, filter)
    }

    private fun unregisterReceiver() {
        if (state != State.NONE) {
            context.unregisterReceiver(wifiActionReceiver)
            state = State.NONE
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun terminateScan() {
        unregisterReceiver()
        disposable?.dispose()
    }
}

private enum class State {
    NONE, ENABLING_AND_SINGLE_UPDATE, ENABLING_AND_MULTI_UPDATE, SCANNING, UPDATED_SCANNING
}

enum class Error(val reason: String) {
    RESULTS_FAILED("Scan result returned false"),
    TIMEOUT("Timeout exception"),
    SCAN_FAILED("Scan failed. Is location service enabled?"),
    WIFI_ENABLING_ERROR("Error while enabling wifi"),
    ENABLED_WIFI_REQUIRED("Enabled wifi required"),
    LOCATION_PERMISSION_REQUIRED("Location permissions needed"),
    WIFI_NOT_READY("Wifi is not ready yet"),
    ALREADY_RUN("Scan is already started")
}

interface OnWifiScanCallback {
    fun onSuccess(data: List<ScannerResult>)
    fun onUnsuccessful(e: Error)
}