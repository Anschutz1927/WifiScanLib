package com.example.networkviewer

import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.scan_library.*
import kotlinx.android.synthetic.main.main_activity.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 42
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        recycler.layoutManager = LinearLayoutManager(this)
        val adapter = ViewerAdapter()
        recycler.adapter = adapter
        val callback = object : OnWifiScanCallback {
            override fun onSuccess(data: List<ScanResult>) {
                if (button_update.isEnabled) {
                    onScanFinished()
                }
                adapter.data = data
            }

            override fun onUnsuccessful(e: Error) {
                onScanFinished()
                Toast.makeText(this@MainActivity, e.reason, Toast.LENGTH_SHORT).show()
                if (toggle_update.checkedButtonId != View.NO_ID) {
                    toggle_update.clearChecked()
                    button_update.isEnabled = true
                }
            }
        }
        val scanner = Scanner(this, lifecycle, callback)
        button_update.setOnClickListener {
            onScanStarted()
            scanner.terminateScan()
            scanner.performWifiScan()
        }
        toggle_update.addOnButtonCheckedListener { _, _, isChecked ->
            scanner.terminateScan()
            if (isChecked) {
                onScanStarted()
                button_update.isEnabled = false
                scanner.performWifiScanWithUpdates()
            } else {
                onScanFinished()
                button_update.isEnabled = true
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!checkScannerPermissions(this)) {
            requestPermissions()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    onPermissionsNotGranted()
                    break
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun requestPermissions() {
        requestScannerPermissions(this, PERMISSIONS_REQUEST_CODE)
    }

    private fun onPermissionsNotGranted() {
        finish()
        Toast.makeText(applicationContext, R.string.permissions_notification, Toast.LENGTH_SHORT)
            .show()
    }

    private fun onScanStarted() {
        progress.visibility = View.VISIBLE
    }

    private fun onScanFinished() {
        progress.visibility = View.GONE
    }
}