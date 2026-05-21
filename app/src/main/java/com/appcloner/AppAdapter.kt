package com.appcloner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.appcloner.core.model.AppInfo
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

class AppAdapter(
    private val onAppClick: (AppInfo) -> Unit,
    private val onCloneClick: (AppInfo) -> Unit = {},
    private val onFavoriteClick: (AppInfo) -> Unit = {}
) : ListAdapter<AppInfo, AppAdapter.AppViewHolder>(AppDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_md3, parent, false)
        return AppViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appName: TextView? = itemView.findViewById(R.id.appName)
        private val packageName: TextView? = itemView.findViewById(R.id.packageName)
        private val appIcon: ImageView? = itemView.findViewById(R.id.appIcon)
        private val versionInfo: TextView? = itemView.findViewById(R.id.version)
        private val appSizeInfo: TextView? = itemView.findViewById(R.id.appSize)
        private val systemAppChip: Chip? = itemView.findViewById(R.id.systemAppChip)
        private val clonedChip: Chip? = itemView.findViewById(R.id.clonedChip)
        private val favoriteButton: MaterialButton? = itemView.findViewById(R.id.favoriteButton)
        private val cloneButton: MaterialButton? = itemView.findViewById(R.id.cloneButton)
        
        fun bind(app: AppInfo) {
            appName?.text = app.appName.ifEmpty { "Unknown App" }
            packageName?.text = app.packageName.ifEmpty { "Unknown Package" }
            
            appIcon?.setImageResource(android.R.drawable.ic_menu_gallery)
            
            versionInfo?.text = app.versionName
            
            // 显示应用大小
            appSizeInfo?.text = app.getFormattedSize()
            
            // 显示系统应用标签
            systemAppChip?.visibility = if (app.isSystemApp) View.VISIBLE else View.GONE
            
            // 显示已克隆标签
            clonedChip?.visibility = if (app.isCloned) View.VISIBLE else View.GONE
            
            // 设置收藏按钮图标
            favoriteButton?.setIconResource(
                if (app.isFavorite) android.R.drawable.star_on 
                else android.R.drawable.star_off
            )
            
            favoriteButton?.setOnClickListener {
                onFavoriteClick(app)
            }
            
            cloneButton?.setOnClickListener {
                onCloneClick(app)
            }
            
            itemView.setOnClickListener {
                onAppClick(app)
            }
        }
    }
    
    private class AppDiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName
        }
        
        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName &&
                   oldItem.appName == newItem.appName &&
                   oldItem.versionName == newItem.versionName &&
                   oldItem.isFavorite == newItem.isFavorite &&
                   oldItem.isCloned == newItem.isCloned &&
                   oldItem.isSystemApp == newItem.isSystemApp
        }
    }
}
