package com.gomizplayer.iptv.core.secrets

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Stores the app-lock PIN using PBKDF2WithHmacSHA256 with a random 16-byte salt
 * and 120,000 iterations (OWASP-style hardening). The raw PIN is never persisted.
 *
 * Migration: legacy SHA-256 hashes (KEY_PIN_HASH) are accepted once and silently
 * upgraded to the hardened format on the next successful verification.
 */
object PinStore {
    private const val PREFS = "gomiz_lock"
    private const val KEY_PIN_HASH = "pin_hash"          // legacy SHA-256 (migration only)
    private const val KEY_PIN_PBKDF2 = "pin_pbkdf2"      // "iterations$salt$hash" (Base64)
    private const val KEY_BIOMETRIC = "biometric_enabled"
    private const val KEY_ATTEMPTS = "pin_attempts"
    private const val KEY_LOCKED_UNTIL = "pin_locked_until"

    private const val ITERATIONS = 120_000
    private const val SALT_BYTES = 16
    private const val HASH_BYTES = 32
    private const val MAX_ATTEMPTS = 5
    private const val LOCK_MS = 30_000L
    private val random = SecureRandom()

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun hasPin(ctx: Context): Boolean =
        prefs(ctx).getString(KEY_PIN_PBKDF2, null)?.isNotBlank() == true ||
        prefs(ctx).getString(KEY_PIN_HASH, null)?.isNotBlank() == true

    fun setPin(ctx: Context, pin: String) {
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val hash = pbkdf2(pin, salt, ITERATIONS)
        val stored = listOf(
            ITERATIONS.toString(),
            Base64.encodeToString(salt, Base64.NO_WRAP),
            Base64.encodeToString(hash, Base64.NO_WRAP)
        ).joinToString("$")
        prefs(ctx).edit()
            .putString(KEY_PIN_PBKDF2, stored)
            .remove(KEY_PIN_HASH)
            .remove(KEY_ATTEMPTS)
            .remove(KEY_LOCKED_UNTIL)
            .apply()
    }

    fun removePin(ctx: Context) {
        prefs(ctx).edit()
            .remove(KEY_PIN_PBKDF2)
            .remove(KEY_PIN_HASH)
            .remove(KEY_ATTEMPTS)
            .remove(KEY_LOCKED_UNTIL)
            .apply()
    }

    /** Returns remaining lock time in ms, or 0 if not locked. */
    fun lockedForMs(ctx: Context): Long {
        val until = prefs(ctx).getLong(KEY_LOCKED_UNTIL, 0L)
        return if (until > System.currentTimeMillis()) until - System.currentTimeMillis() else 0L
    }

    fun verify(ctx: Context, pin: String): Boolean {
        val p = prefs(ctx)
        if (lockedForMs(ctx) > 0) return false

        // New hardened format
        val stored = p.getString(KEY_PIN_PBKDF2, null)
        if (!stored.isNullOrBlank()) {
            val parts = stored.split("$")
            if (parts.size == 3) {
                val ok = try {
                    val iter = parts[0].toInt()
                    val salt = Base64.decode(parts[1], Base64.NO_WRAP)
                    val expected = Base64.decode(parts[2], Base64.NO_WRAP)
                    MessageDigest.isEqual(expected, pbkdf2(pin, salt, iter))
                } catch (_: Exception) { false }
                if (ok) { resetAttempts(ctx); return true }
                registerFailure(ctx)
                return false
            }
        }

        // Legacy SHA-256 migration path
        val legacy = p.getString(KEY_PIN_HASH, null)
        if (!legacy.isNullOrBlank()) {
            val ok = MessageDigest.isEqual(
                legacy.toByteArray(Charsets.UTF_8),
                sha256(pin).toByteArray(Charsets.UTF_8)
            )
            if (ok) {
                setPin(ctx, pin)   // silently upgrade to PBKDF2
                resetAttempts(ctx)
                return true
            }
            registerFailure(ctx)
        }
        return false
    }

    private fun registerFailure(ctx: Context) {
        val p = prefs(ctx)
        val n = p.getInt(KEY_ATTEMPTS, 0) + 1
        if (n >= MAX_ATTEMPTS) {
            p.edit().putInt(KEY_ATTEMPTS, 0)
                .putLong(KEY_LOCKED_UNTIL, System.currentTimeMillis() + LOCK_MS).apply()
        } else {
            p.edit().putInt(KEY_ATTEMPTS, n).apply()
        }
    }

    private fun resetAttempts(ctx: Context) {
        prefs(ctx).edit().remove(KEY_ATTEMPTS).remove(KEY_LOCKED_UNTIL).apply()
    }

    private fun pbkdf2(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, HASH_BYTES * 8)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun sha256(pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(pin.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    fun biometricEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_BIOMETRIC, false)

    fun setBiometric(ctx: Context, enabled: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
    }
}
