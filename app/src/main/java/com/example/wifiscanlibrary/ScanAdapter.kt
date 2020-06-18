package com.example.wifiscanlibrary

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.scan_library.ScannerResult
import kotlinx.android.synthetic.main.view_item.view.*

class ScanAdapter : RecyclerView.Adapter<ScanAdapter.Holder>() {

    var data: List<ScannerResult> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_item, parent, false)
        return Holder(view)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.itemView.text_name.text = data[position].ssid
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view)
}