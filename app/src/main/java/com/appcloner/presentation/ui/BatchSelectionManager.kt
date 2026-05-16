
package com.appcloner.presentation.ui

import android.content.Context
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity

class BatchSelectionManager(
    private val activity: AppCompatActivity,
    private val onSelectionChanged: (Int) -> Unit,
    private val onBatchAction: (BatchAction) -> Unit
) : ActionMode.Callback {

    private var actionMode: ActionMode? = null
    private val selectedItems = mutableSetOf<String>()

    enum class BatchAction {
        SAVE, LAUNCH, DELETE, SHARE
    }

    fun startSelectionMode() {
        if (actionMode == null) {
            actionMode = activity.startActionMode(this)
        }
    }

    fun stopSelectionMode() {
        actionMode?.finish()
        actionMode = null
        selectedItems.clear()
    }

    fun toggleSelection(packageName: String) {
        if (selectedItems.contains(packageName)) {
            selectedItems.remove(packageName)
        } else {
            selectedItems.add(packageName)
        }
        
        onSelectionChanged(selectedItems.size)
        actionMode?.title = "已选择 ${selectedItems.size} 项"
        
        if (selectedItems.isEmpty()) {
            stopSelectionMode()
        }
    }

    fun isSelected(packageName: String): Boolean {
        return selectedItems.contains(packageName)
    }

    fun getSelectedCount(): Int {
        return selectedItems.size
    }

    fun getSelectedItems(): Set<String> {
        return selectedItems.toSet()
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        menu?.add(0, 1, 0, "保存APK")
        menu?.add(0, 2, 0, "启动应用")
        menu?.add(0, 3, 0, "分享APK")
        menu?.add(0, 4, 0, "删除")
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        when (item?.itemId) {
            1 -> {
                onBatchAction(BatchAction.SAVE)
                stopSelectionMode()
                return true
            }
            2 -> {
                onBatchAction(BatchAction.LAUNCH)
                stopSelectionMode()
                return true
            }
            3 -> {
                onBatchAction(BatchAction.SHARE)
                stopSelectionMode()
                return true
            }
            4 -> {
                onBatchAction(BatchAction.DELETE)
                stopSelectionMode()
                return true
            }
        }
        return false
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        selectedItems.clear()
        onSelectionChanged(0)
        actionMode = null
    }
}
