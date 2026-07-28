package com.autofor.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autofor.data.ForwardingRule
import com.autofor.data.RuleRepository
import com.autofor.scheduler.ScheduleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isGlobalEnabled: Boolean = true,
    val lastStatus: String = "Inactive (Forwarding OFF)",
    val rules: List<ForwardingRule> = emptyList(),
    val isLoading: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RuleRepository(application)
    private val scheduleManager = ScheduleManager(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
        scheduleManager.checkAndSyncActiveState()
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState(
                isGlobalEnabled = repository.isGlobalEnabled(),
                lastStatus = repository.getLastForwardingStatus(),
                rules = repository.getRules()
            )
        }
    }

    fun setGlobalEnabled(enabled: Boolean) {
        repository.setGlobalEnabled(enabled)
        scheduleManager.rescheduleAll()
        scheduleManager.checkAndSyncActiveState()
        loadData()
    }

    fun addOrUpdateRule(rule: ForwardingRule) {
        repository.addOrUpdateRule(rule)
        scheduleManager.rescheduleAll()
        scheduleManager.checkAndSyncActiveState()
        loadData()
    }

    fun deleteRule(ruleId: String) {
        repository.deleteRule(ruleId)
        scheduleManager.rescheduleAll()
        scheduleManager.checkAndSyncActiveState()
        loadData()
    }

    fun toggleRule(rule: ForwardingRule, enabled: Boolean) {
        val updated = rule.copy(isEnabled = enabled)
        repository.addOrUpdateRule(updated)
        scheduleManager.rescheduleAll()
        scheduleManager.checkAndSyncActiveState()
        loadData()
    }

    fun updateStatus(status: String) {
        repository.setLastForwardingStatus(status)
        loadData()
    }
}
