package com.gomizplayer.iptv

import android.app.Application
import android.content.Context
import com.gomizplayer.iptv.core.store.Store
import com.gomizplayer.iptv.data.EpgRepository
import com.gomizplayer.iptv.data.LibraryRepository
import com.gomizplayer.iptv.data.MediaDownloadRepository
import com.gomizplayer.iptv.data.WidgetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/** Service locator â€” wires store/repositories without a DI framework. */
class GomizApp : Application(), coil3.SingletonImageLoader.Factory {

    lateinit var store: Store
        private set
    lateinit var library: LibraryRepository
        private set
    lateinit var epg: EpgRepository
        private set
    lateinit var widget: WidgetRepository
        private set
    lateinit var media: MediaDownloadRepository
        private set

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Coil: SVG logos (Wikimedia) â€” decoders auto-register via ServiceLoader; crossfade enabled.
        coil3.SingletonImageLoader.setSafe(this)
        store = Store(this)
        library = LibraryRepository(this, store)
        epg = EpgRepository(this, store)
        widget = WidgetRepository(store)
        media = MediaDownloadRepository(this)
        // Warm caches, then refresh sources in the background.
        appScope.launch {
            library.loadCache()
            library.refreshAll()
        }
        appScope.launch {
            epg.loadCache()
            epg.refreshAll()
        }
        appScope.launch {
            widget.refresh(force = true)
            while (true) {
                delay(WidgetRepository.REFRESH_MS)
                widget.refresh()
            }
        }
    }

    override fun newImageLoader(context: Context): coil3.ImageLoader =
        coil3.ImageLoader.Builder(context)
            .components { add(coil3.svg.SvgDecoder.Factory()) }
            .build()
}

fun gomiz(context: android.content.Context): GomizApp =
    context.applicationContext as GomizApp
