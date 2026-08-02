package com.example.app_gim

import android.content.Context

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("gym_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_ROLE = "role"
    }

    fun saveSession(token: String, userId: Int, fullName: String, role: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_FULL_NAME, fullName)
            .putString(KEY_ROLE, role)
            .apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getFullName(): String? = prefs.getString(KEY_FULL_NAME, null)
    fun getRole(): String? = prefs.getString(KEY_ROLE, null)
    fun isAdmin(): Boolean = getRole() == "ADMIN"
    fun isLoggedIn(): Boolean = getToken() != null

    fun clearSession() {
        prefs.edit().clear().apply()
    }
    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)
}