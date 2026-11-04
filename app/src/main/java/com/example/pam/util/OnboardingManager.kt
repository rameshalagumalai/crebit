package com.example.pam.util

import android.content.Context
import android.content.SharedPreferences

object OnboardingManager {
    private const val PREFS_NAME = "pam_onboarding_prefs"
    private const val KEY_WALKTHROUGH_COMPLETED = "walkthrough_completed"

    private var prefs: SharedPreferences? = null

    private fun getPrefs(context: Context): SharedPreferences {
        return prefs ?: context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).also {
            prefs = it
        }
    }

    fun isWalkthroughCompleted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_WALKTHROUGH_COMPLETED, false)
    }

    fun setWalkthroughCompleted(context: Context, completed: Boolean = true) {
        getPrefs(context).edit().putBoolean(KEY_WALKTHROUGH_COMPLETED, completed).apply()
    }

    fun resetWalkthrough(context: Context) {
        getPrefs(context).edit().remove(KEY_WALKTHROUGH_COMPLETED).apply()
    }
}

