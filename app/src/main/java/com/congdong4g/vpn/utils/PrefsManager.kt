package com.congdong4g.vpn.utils

import android.content.Context
import android.content.SharedPreferences
import com.congdong4g.vpn.model.ConnectionHistory
import com.congdong4g.vpn.model.Server
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PrefsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("congdong4g_vpn", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_AUTO_CONNECT = "auto_connect"
        private const val KEY_SELECTED_SERVER = "selected_server"
        private const val KEY_CONNECTION_HISTORY = "connection_history"
        private const val KEY_SUBSCRIBE_URL = "subscribe_url"
        private const val KEY_VPN_CONFIG = "vpn_config"
        private const val KEY_LAST_CONNECTED_SERVER = "last_connected_server"
    }

    // Token
    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    // Email
    var email: String?
        get() = prefs.getString(KEY_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_EMAIL, value).apply()

    // Dark Mode
    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    // Auto Connect
    var isAutoConnect: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CONNECT, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CONNECT, value).apply()

    // Selected Server
    var selectedServer: Server?
        get() {
            val json = prefs.getString(KEY_SELECTED_SERVER, null) ?: return null
            return gson.fromJson(json, Server::class.java)
        }
        set(value) {
            val json = if (value != null) gson.toJson(value) else null
            prefs.edit().putString(KEY_SELECTED_SERVER, json).apply()
        }

    // Last Connected Server
    var lastConnectedServer: Server?
        get() {
            val json = prefs.getString(KEY_LAST_CONNECTED_SERVER, null) ?: return null
            return gson.fromJson(json, Server::class.java)
        }
        set(value) {
            val json = if (value != null) gson.toJson(value) else null
            prefs.edit().putString(KEY_LAST_CONNECTED_SERVER, json).apply()
        }

    // Subscribe URL
    var subscribeUrl: String?
        get() = prefs.getString(KEY_SUBSCRIBE_URL, null)
        set(value) = prefs.edit().putString(KEY_SUBSCRIBE_URL, value).apply()

    // VPN Config
    var vpnConfig: String?
        get() = prefs.getString(KEY_VPN_CONFIG, null)
        set(value) = prefs.edit().putString(KEY_VPN_CONFIG, value).apply()

    // Connection History
    fun getConnectionHistory(): List<ConnectionHistory> {
        val json = prefs.getString(KEY_CONNECTION_HISTORY, null) ?: return emptyList()
        val type = object : TypeToken<List<ConnectionHistory>>() {}.type
        return gson.fromJson(json, type)
    }

    fun addConnectionHistory(history: ConnectionHistory) {
        val list = getConnectionHistory().toMutableList()
        list.add(0, history)
        // Keep only last 50 records
        if (list.size > 50) {
            list.subList(50, list.size).clear()
        }
        prefs.edit().putString(KEY_CONNECTION_HISTORY, gson.toJson(list)).apply()
    }

    fun clearConnectionHistory() {
        prefs.edit().remove(KEY_CONNECTION_HISTORY).apply()
    }

    // Is Logged In
    fun isLoggedIn(): Boolean = token != null

    // Logout
    fun logout() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_EMAIL)
            .remove(KEY_SUBSCRIBE_URL)
            .remove(KEY_VPN_CONFIG)
            .remove(KEY_SELECTED_SERVER)
            .apply()
    }

    // Clear All
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
