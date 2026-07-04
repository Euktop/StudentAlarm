package com.euktop.studentalarm

class AlarmModeManager {

    var isSelectionMode: Boolean = false
        private set

    var selectedIds: Set<Long> = emptySet()
        private set

    fun enableSelectionMode() {
        isSelectionMode = true
    }

    fun disableSelectionMode() {
        isSelectionMode = false
        selectedIds = emptySet()
    }

    fun toggleSelection(alarmId: Long) {
        selectedIds = if (selectedIds.contains(alarmId)) {
            selectedIds - alarmId
        } else {
            selectedIds + alarmId
        }
    }

    fun isSelected(alarmId: Long): Boolean {
        return selectedIds.contains(alarmId)
    }

    fun clearSelection() {
        selectedIds = emptySet()
    }
}