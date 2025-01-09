package com.reishandy.guestbook.data

import android.content.Context
import android.content.SharedPreferences

object GuestBookPreferenceManager {
    private const val PREF_NAME = "AppPreferences"
    private const val KEY_BASE_URL = ""

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setBaseUrl(context: Context, baseUrl: String) {
        val editor = getPreferences(context).edit()
        editor.putString(KEY_BASE_URL, baseUrl)
        editor.apply()
    }

    fun getBaseUrl(context: Context, defaultUrl: String = "https://reishandy.my.id"): String {
        return getPreferences(context).getString(KEY_BASE_URL, defaultUrl) ?: defaultUrl
    }
}