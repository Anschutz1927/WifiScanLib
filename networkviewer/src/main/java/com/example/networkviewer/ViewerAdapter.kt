package com.example.networkviewer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.scan_library.ScannerResult
import kotlinx.android.synthetic.main.item_details.view.*

class ViewerAdapter : RecyclerView.Adapter<ViewerAdapter.Holder>() {

    var data: List<ScannerResult> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_details, parent, false)
    )

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = data[position]
        holder.itemView.text_ssid.text = item.ssid
        holder.itemView.text_strength.text = item.signalStrength.toString()
        val isEncrypted = item.isLocked
        val accessType = if (isEncrypted) "Encrypted" else "Opened"
        holder.itemView.text_ssid.append(" ($accessType)")
        holder.itemView.checkbox_locked.isChecked = isEncrypted
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view)
}
