package com.example.networkviewer.fragment

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.networkviewer.R
import com.example.networkviewer.adapter.ViewerAdapter
import com.example.networkviewer.viewmodel.MainViewModel
import com.example.scan_library.utils.checkScannerPermissions
import com.example.scan_library.utils.requestScannerPermissions
import kotlinx.android.synthetic.main.main_fragment.*

class MainFragment : Fragment() {

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 42
    }

    private val adapter = ViewerAdapter()
    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        initViews()
        subscribeToUpdates()
    }

    override fun onStart() {
        super.onStart()
        context?.let {
            if (!checkScannerPermissions(it)) {
                requestPermissions()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.isLoading.removeObservers(this)
        viewModel.data.removeObservers(this)
        viewModel.error.removeObserver(this)
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
        activity?.let { requestScannerPermissions(it, PERMISSIONS_REQUEST_CODE) }
    }

    private fun onPermissionsNotGranted() {
        activity?.let {
            it.finish()
            Toast.makeText(
                it.applicationContext,
                R.string.permissions_notification,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun initViews() {
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = adapter
        button_update.setOnClickListener { viewModel.onSingleScanClicked() }
        toggle_update.addOnButtonCheckedListener { _, _, isChecked ->
            if (isChecked) {
                viewModel.onIntervalScanClicked()
            } else {
                viewModel.terminateScan()
            }
        }
    }

    private fun subscribeToUpdates() {
        viewModel.isLoading.observe(this, Observer {
            progress.visibility = when (it) {
                true -> View.VISIBLE
                else -> View.GONE
            }
            button_update.isEnabled = !it
        })
        viewModel.data.observe(this, Observer { adapter.data = it })
        viewModel.error.setObserver(this, Observer {
            it?.let {
                Toast.makeText(context, it.reason, Toast.LENGTH_SHORT).show()
                if (toggle_update.checkedButtonId != View.NO_ID) {
                    toggle_update.clearChecked()
                }
            }
        })
    }
}