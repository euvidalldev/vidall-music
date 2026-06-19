package com.example.data

import android.content.Context
import android.content.SharedPreferences

object InstanceConfig {
    private const val PREFS_NAME = "cobalt_config"
    private const val KEY_INSTANCE_URL = "custom_instance_url"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_AUTH_SCHEME = "auth_scheme"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var customInstanceUrl: String?
        get() = prefs?.getString(KEY_INSTANCE_URL, null)
        set(value) { prefs?.edit()?.putString(KEY_INSTANCE_URL, value)?.apply() }

    var apiKey: String?
        get() = prefs?.getString(KEY_API_KEY, null)
        set(value) { prefs?.edit()?.putString(KEY_API_KEY, value)?.apply() }

    var authScheme: String?
        get() = prefs?.getString(KEY_AUTH_SCHEME, null)
        set(value) { prefs?.edit()?.putString(KEY_AUTH_SCHEME, value)?.apply() }

    fun hasCustomInstance(): Boolean = !customInstanceUrl.isNullOrBlank()

    fun hasAuth(): Boolean = !apiKey.isNullOrBlank()

    fun clear() {
        prefs?.edit()?.clear()?.apply()
    }
}
