
package com.appcloner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private val appList: List&lt;AppInfo&gt;,
    private val onSaveClick: (AppInfo) -&gt; Unit,
    private val onShareClick: (AppInfo) -&gt; Unit,
    private val onInstallClick: (AppInfo) -&gt; Unit,
    private val onDeleteClick: (AppInfo) -&gt; Unit,
    private val onLaunchClick: (AppInfo) -&gt; Unit,
    private val onDetailClick: (AppInfo) -&gt; Unit
) : RecyclerView.Adapter&lt;AppAdapter.AppViewHolder&gt;() {

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appIcon: ImageView = itemView.findViewById(R.id.appIcon)
        val appName: TextView = itemView.findViewById(R.id.appName)
        val packageName: TextView = itemView.findViewById(R.id.packageName)
        val actionButton1: Button = itemView.findViewById(R.id.actionButton1)
        val actionButton2: Button? = itemView.findViewById(R.id.actionButton2)
        val actionButton3: Button? = itemView.findViewById(R.id.actionButton3)
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
            holder.appName.text = app.appName
            holder.packageName.text = app.packageName
            
            holder.actionButton1.text = "安装"
            holder.actionButton2?.text = "分享"
            holder.actionButton3?.text = "详情"
            
            holder.actionButton2?.visibility = View.VISIBLE
            holder.actionButton3?.visibility = View.VISIBLE
            
            holder.actionButton1.setOnClickListener {
                onInstallClick(app)
            }
            
            holder.actionButton2?.setOnClickListener {
                onShareClick(app)
            }
            
            holder.actionButton3?.setOnClickListener {
                onDetailClick(app)
            }
            
            holder.itemView.setOnLongClickListener {
                onDeleteClick(app)
                true
            }
        } else {
            holder.appName.text = app.appName
            holder.packageName.text = "${app.packageName}\n${app.versionName} - ${app.getFormattedSize()}"
            holder.actionButton1.text = "克隆"
            holder.actionButton2?.text = "启动"
            holder.actionButton3?.text = "详情"
            
            holder.actionButton2?.visibility = View.VISIBLE
            holder.actionButton3?.visibility = View.VISIBLE
            
            holder.actionButton1.setOnClickListener {
                onSaveClick(app)
            }
            
            holder.actionButton2?.setOnClickListener {
                onLaunchClick(app)
            }
            
            holder.actionButton3?.setOnClickListener {
                onDetailClick(app)
            }
            
            holder.itemView.setOnLongClickListener(null)
        }
    }

    override fun getItemCount(): Int = appList.size
}
