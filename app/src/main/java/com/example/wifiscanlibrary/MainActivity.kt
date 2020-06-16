package com.example.wifiscanlibrary

import android.net.wifi.ScanResult
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.scan_library.OnWifiScanCallback
import com.example.scan_library.Scanner
import com.example.scan_library.checkScannerPermissions
import com.example.scan_library.requestScannerPermissions
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var scanner: Scanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val scanAdapter = ScanAdapter()
        recycler.adapter = scanAdapter
        recycler.layoutManager = LinearLayoutManager(this)
        val scanCallback = object : OnWifiScanCallback {
            override fun onSuccess(data: List<ScanResult>) {
                scanAdapter.data = data
            }

            override fun onUnsuccessful(e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Unsuccessful [${e.localizedMessage}]",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        scanner = Scanner(this, lifecycle, scanCallback)
        button.setOnClickListener {
            scanner.performWifiScan()
        }
    }

    override fun onStart() {
        super.onStart()
        if (!checkScannerPermissions(this)) {
            requestScannerPermissions(this, 42)
        }
    }
}