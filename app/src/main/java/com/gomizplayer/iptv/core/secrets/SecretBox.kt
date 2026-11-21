package com.gomizplayer.iptv.core.secrets

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Keystore-backed AES/GCM encryption for Xtream source passwords.
 * Windows equivalent: safeStorage + secrets.js (prepareSourceSecrets/decryptSourcePassword).
 */
object SecretBox {
    private const val ALIAS = "gomiz_source_credentials"
    private const val PROVIDER = "AndroidKeyStore"
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12

    private fun keyStore(): KeyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }

    private fun getKey(): SecretKey {
        val ks = keyStore()
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        gen.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    /** Returns base64(iv || ciphertext). */
    fun encrypt(plain: String): String {
        if (plain.isEmpty()) return ""
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val out = ByteArray(cipher.iv.size + ct.size)
        System.arraycopy(cipher.iv, 0, out, 0, cipher.iv.size)
        System.arraycopy(ct, 0, out, cipher.iv.size, ct.size)
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    /** Decrypts base64(iv || ciphertext); returns null if it cannot be decrypted. */
    fun decrypt(encoded: String): String? {
        if (encoded.isBlank()) return null
        return try {
            val all = Base64.decode(encoded, Base64.NO_WRAP)
            if (all.size <= IV_BYTES) return null
            val iv = all.copyOfRange(0, IV_BYTES)
            val ct = all.copyOfRange(IV_BYTES, all.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(ct), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }
}
