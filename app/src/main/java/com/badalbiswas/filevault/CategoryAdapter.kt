package com.badalbiswas.filevault

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(
    private val items: List<CategoryItem>,
    private val onClick: (CategoryItem) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bg: View = view.findViewById(R.id.catBg)
        val icon: ImageView = view.findViewById(R.id.catIcon)
        val label: TextView = view.findViewById(R.id.catLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.label.text = item.label
        holder.icon.setImageResource(item.iconRes)
        holder.bg.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(holder.itemView.context, item.colorRes)
        )
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}
