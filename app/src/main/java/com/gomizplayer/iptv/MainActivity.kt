package com.gomizplayer.iptv

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gomizplayer.iptv.ui.AppRoot
import java.io.File
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(applyStoredLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppRoot() }
    }

    override fun onStop() {
        // Flush any debounced store writes before the process may be suspended.
        try { (application as GomizApp).store.flush() } catch (_: Exception) {}
        super.onStop()
    }

    private fun applyStoredLocale(base: Context): Context {
        val lang = readStoredLanguage(base) ?: return base
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    /** Reads "language" from gomizplayer.json WITHOUT constructing the full store. */
    private fun readStoredLanguage(base: Context): String? {
        return try {
            val file = File(base.filesDir, "gomizplayer.json")
            if (!file.exists()) return null
            val text = file.readText(Charsets.UTF_8)
            Regex("\"language\"\\s*:\\s*\"([a-zA-Z-]{2,8})\"").find(text)?.groupValues?.getOrNull(1)
        } catch (_: Exception) {
            null
        }
    }
}
