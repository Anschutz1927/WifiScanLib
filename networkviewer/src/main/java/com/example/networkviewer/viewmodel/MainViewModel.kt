package com.example.networkviewer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.scan_library.Error
import com.example.scan_library.OnWifiScanCallback
import com.example.scan_library.Scanner
import com.example.scan_library.ScannerResult

class MainViewModel(application: Application) : AndroidViewModel(application), OnWifiScanCallback {

    val isLoading = MutableLiveData(false)
    val data: MutableLiveData<List<ScannerResult>> = MutableLiveData(emptyList())
    val error: SingleMutableLiveData<Error> = SingleMutableLiveData()
    private val scanner: Scanner = Scanner(context = application, callback = this)

    override fun onSuccess(data: List<ScannerResult>) {
        this.data.value = data
        if (!scanner.isScanning()) {
            onScanFinished()
        }
    }

    override fun onUnsuccessful(e: Error) {
        this.error.setValue(e)
        onScanFinished()
    }

    override fun onCleared() {
        scanner.terminateScan()
        super.onCleared()
    }

    fun onSingleScanClicked() {
        scanner.terminateScan()
        onScanStarted()
        scanner.performWifiScan()
    }

    fun onIntervalScanClicked() {
        scanner.terminateScan()
        onScanStarted()
        scanner.performWifiScanWithUpdates()
    }

    fun terminateScan() {
        scanner.terminateScan()
        onScanFinished()
    }

    private fun onScanStarted() {
        isLoading.value = true
    }

    private fun onScanFinished() {
        isLoading.value = false
    }
}

class SingleMutableLiveData<T>(value: T? = null) {

    private val liveData = MutableLiveData(value)
    private var hasValueSent = false

    fun getValue(): T? {
        return liveData.value
    }

    fun setValue(value: T) {
        hasValueSent = false
        liveData.value = value
    }

    fun setObserver(owner: LifecycleOwner, observer: Observer<in T>) {
        if (liveData.hasObservers()) throw IllegalStateException("Previous observer should be removed first!")
        liveData.observe(owner, Observer {
            if (!hasValueSent) {
                observer.onChanged(it)
            }
        })
    }

    fun removeObserver(owner: LifecycleOwner) {
        liveData.removeObservers(owner)
    }
}