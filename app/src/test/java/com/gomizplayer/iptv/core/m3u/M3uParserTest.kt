package com.gomizplayer.iptv.core.m3u

import com.gomizplayer.iptv.core.classify.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {

    private val sample = """
        #EXTM3U
        #EXTINF:-1 tvg-id="aje" tvg-name="Al Jazeera" tvg-logo="http://img/aje.png" group-title="News",Al Jazeera English
        https://live.example.com/aje.m3u8
        #EXTINF:-1 tvg-id="mov1" group-title="Movies",Big Buck Bunny
        https://vod.example.com/bbb.mp4
        #EXTINF:-1 group-title="مسلسلات",Turkish Drama S01E02
        https://vod.example.com/show.mp4
        #EXTVLCOPT:http-user-agent=MyAgent/1.0
        #EXTINF:-1 group-title="مخفية|adults",Adult Channel
        https://live.example.com/adult.m3u8
    """.trimIndent()

    @Test fun parsesAllEntries() {
        val items = M3u.parseM3U(sample)
        assertEquals(4, items.size)
    }

    @Test fun entriesCarryAttributesAndKind() {
        val items = M3u.parseM3U(sample)
        val news = items.first { it.tvgId == "aje" }
        assertEquals("Al Jazeera English", news.name)
        assertEquals("News", news.group)
        assertEquals("http://img/aje.png", news.logo)
        assertEquals(Kind.LIVE, news.kind) // .m3u8 playlists are live streams (IPTV groups included)
        val movie = items.first { it.name == "Big Buck Bunny" }
        assertEquals(Kind.MOVIE, movie.kind)
        val series = items.first { it.name.startsWith("Turkish Drama") }
        assertEquals(Kind.SERIES, series.kind)
    }

    @Test fun headersFromExtvlcopt() {
        val items = M3u.parseM3U(sample)
        val adult = items.first { it.name == "Adult Channel" }
        assertEquals("MyAgent/1.0", adult.headers["User-Agent"])
    }

    @Test fun hiddenAdultEntries() {
        val items = M3u.parseM3U(sample)
        assertTrue(items.first { it.name == "Adult Channel" }.hidden)
        assertFalse(items.first { it.name == "Big Buck Bunny" }.hidden)
    }

    @Test fun xtreamUrlBuilding() {
        assertEquals(
            "http://iptv.example.com:8080/get.php?username=user&password=pass&type=m3u_plus&output=ts",
            M3u.xtreamUrl("iptv.example.com:8080", "user", "pass")
        )
        assertEquals(
            "https://iptv.example.com/get.php?username=u&password=p&type=m3u_plus&output=ts",
            M3u.xtreamUrl("https://iptv.example.com/", "u", "p")
        )
    }

    @Test fun xtreamAccountUrlBuilding() {
        assertEquals(
            "http://iptv.example.com/player_api.php?username=user&password=pass",
            M3u.xtreamAccountUrl("iptv.example.com", "user", "pass")
        )
    }

    @Test fun playlistDetection() {
        assertTrue(M3u.looksLikePlaylist("#EXTM3U\n#EXTINF:-1,x\nhttp://x"))
        assertFalse(M3u.isHlsPlaylist("#EXTM3U\n#EXTINF:-1,x\nhttp://x"))
        assertTrue(M3u.isHlsPlaylist("#EXTM3U\n#EXT-X-VERSION:3\n"))
    }

    @Test fun keysAreStable() {
        val a = M3u.itemKey("http://x", "g", "n")
        val b = M3u.itemKey("http://x", "g", "n")
        assertEquals(a, b)
    }
}
