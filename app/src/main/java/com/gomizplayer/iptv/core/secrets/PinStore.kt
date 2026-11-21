package com.gomizplayer.iptv.core.secrets

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

/**
 * Stores a hashed app-lock PIN in SharedPreferences (SHA-256).
 * The raw PIN is never persisted.
 */
object PinStore {
    private const val PREFS = "gomiz_lock"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_BIOMETRIC = "biometric_enabled"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun hasPin(ctx: Context): Boolean =
        prefs(ctx).getString(KEY_PIN_HASH, null)?.isNotBlank() == true

    fun setPin(ctx: Context, pin: String) {
        prefs(ctx).edit().putString(KEY_PIN_HASH, hash(pin)).apply()
    }

    fun removePin(ctx: Context) {
        prefs(ctx).edit().remove(KEY_PIN_HASH).apply()
    }

    fun verify(ctx: Context, pin: String): Boolean {
        val stored = prefs(ctx).getString(KEY_PIN_HASH, null) ?: return false
        return stored == hash(pin)
    }

    fun biometricEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_BIOMETRIC, false)

    fun setBiometric(ctx: Context, enabled: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
    }

    private fun hash(pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
