package com.appcloner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private val appList: List<AppInfo>,
    private val onCloneClick: (AppInfo) -> Unit,
    private val onLaunchClick: (AppInfo) -> Unit,
    private val onDeleteClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appIcon: ImageView = itemView.findViewById(R.id.appIcon)
        val appName: TextView = itemView.findViewById(R.id.appName)
        val packageName: TextView = itemView.findViewById(R.id.packageName)
        val actionButton: Button = itemView.findViewById(R.id.actionButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = appList[position]
        holder.appIcon.setImageDrawable(app.icon)
        
        if (app.isCloned) {
            holder.appName.text = "${app.appName} (分身${app.cloneId})"
            holder.actionButton.text = holder.itemView.context.getString(R.string.launch_app)
            
            holder.actionButton.setOnClickListener {
                onLaunchClick(app)
            }
            
            holder.itemView.setOnLongClickListener {
                onDeleteClick(app)
                true
            }
        } else {
            holder.appName.text = app.appName
            holder.actionButton.text = holder.itemView.context.getString(R.string.clone_app)
            
            holder.actionButton.setOnClickListener {
                onCloneClick(app)
            }
            
            holder.itemView.setOnLongClickListener(null)
        }
        
        holder.packageName.text = app.packageName
    }

    override fun getItemCount(): Int = appList.size
}
