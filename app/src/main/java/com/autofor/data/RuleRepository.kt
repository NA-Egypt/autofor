package com.autofor.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class RuleRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("autofor_rules_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getRules(): List<ForwardingRule> {
        val json = prefs.getString("rules_list", null) ?: return emptyList()
        val type = object : TypeToken<List<ForwardingRule>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveRules(rules: List<ForwardingRule>) {
        val json = gson.toJson(rules)
        prefs.edit().putString("rules_list", json).apply()
    }

    fun addOrUpdateRule(rule: ForwardingRule) {
        val current = getRules().toMutableList()
        val index = current.indexOfFirst { it.id == rule.id }
        if (index >= 0) {
            current[index] = rule
        } else {
            current.add(rule)
        }
        saveRules(current)
    }

    fun deleteRule(ruleId: String) {
        val current = getRules().filterNot { it.id == ruleId }
        saveRules(current)
    }

    fun isGlobalEnabled(): Boolean {
        return prefs.getBoolean("global_enabled", true)
    }

    fun setGlobalEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("global_enabled", enabled).apply()
    }

    fun getLastForwardingStatus(): String {
        return prefs.getString("last_status", "Inactive (Forwarding OFF)") ?: "Inactive (Forwarding OFF)"
    }

    fun setLastForwardingStatus(status: String) {
        prefs.edit().putString("last_status", status).apply()
    }
}
