package com.example.photosapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.photosapp.data.model.Tool

class ToolsAdapter(private val toolsList: List<Tool>, private val onToolSelected: (Tool) -> Unit) : RecyclerView.Adapter<ToolsAdapter.ToolViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.tool_item, parent, false)
        return ToolViewHolder(view)
    }

    override fun onBindViewHolder(holder: ToolViewHolder, position: Int) {
        val tool = toolsList[position]
        holder.bind(tool)
        holder.itemView.setOnClickListener {
            onToolSelected(tool)
        }
    }

    override fun getItemCount(): Int = toolsList.size

    class ToolViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: ImageView = itemView.findViewById(R.id.toolIcon)
        private val nameView: TextView = itemView.findViewById(R.id.toolName)

        fun bind(tool: Tool) {
            iconView.setImageResource(tool.iconResId)
            nameView.text = tool.name
        }
    }
}
