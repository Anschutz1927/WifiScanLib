package com.example.networkviewer

import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_details.view.*

class ViewerAdapter : RecyclerView.Adapter<ViewerAdapter.Holder>() {

    var data: List<ScanResult> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_details, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = data[position]
        holder.itemView.text_ssid.text = item.SSID
        holder.itemView.text_strength.text =
            WifiManager.calculateSignalLevel(item.level, 100).toString()
        val isEncrypted = item.capabilities.toString()
            .replace("[ESS]", "")
            .replace("[WPS]", "")
            .isNotEmpty()
        val accessType = if (isEncrypted) "Encrypted" else "Opened"
        holder.itemView.text_ssid.append(" ($accessType)")
        holder.itemView.checkbox_locked.isChecked = isEncrypted
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view)
}
