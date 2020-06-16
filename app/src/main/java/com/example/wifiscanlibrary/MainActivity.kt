package com.example.wifiscanlibrary

import android.net.wifi.ScanResult
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.scan_library.*
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

            override fun onUnsuccessful(e: Error) {
                Toast.makeText(
                    this@MainActivity,
                    "Unsuccessful [${e.reason}]",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        scanner = Scanner(this, lifecycle, scanCallback)
        scanAdapter.data = scanner.getLastSuccessResult()
        var counter = 0
        button_refresh.setOnClickListener {
            if (counter < 5) {
                scanner.terminateScan()
                scanner.performWifiScan()
                counter++
                if (counter == 5) {
                    lifecycle.removeObserver(scanner)
                }
            }
        }
        button_subscribe.setOnClickListener {
            scanner.terminateScan()
            scanner.performWifiScanWithUpdates()
        }
    }

    override fun onStart() {
        super.onStart()
        if (!checkScannerPermissions(this)) {
            requestScannerPermissions(this, 42)
        }
    }
}