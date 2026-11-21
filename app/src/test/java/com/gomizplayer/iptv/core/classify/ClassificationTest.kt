package com.gomizplayer.iptv.core.classify

import org.junit.Assert.assertEquals
import org.junit.Test

class ClassificationTest {

    private fun kind(url: String = "", group: String = "", name: String = "", tvgName: String = "", type: String = "", kindField: String = "") =
        Classification.classifyMedia(MediaMeta(url = url, group = group, name = name, tvgName = tvgName, type = type, kind = kindField))

    // ---- URL explicit paths ----
    @Test fun urlLivePathIsLive() = assertEquals(Kind.LIVE, kind(url = "http://example.com/live/123.ts"))
    @Test fun urlMoviePathIsMovie() = assertEquals(Kind.MOVIE, kind(url = "http://example.com/movie/123.mp4"))
    @Test fun urlSeriesPathIsSeries() = assertEquals(Kind.SERIES, kind(url = "http://example.com/series/abc"))
    @Test fun urlStreamTypeParamWins() = assertEquals(Kind.LIVE, kind(url = "http://example.com/stream?stream_type=live"))
    @Test fun urlVodPathIsMovie() = assertEquals(Kind.MOVIE, kind(url = "http://example.com/vod/x"))

    // ---- group-title hints ----
    @Test fun groupMoviesIsMovie() = assertEquals(Kind.MOVIE, kind(group = "Movies"))
    @Test fun groupLiveTvIsLive() = assertEquals(Kind.LIVE, kind(group = "Live TV"))
    @Test fun groupArabicSeriesIsSeries() = assertEquals(Kind.SERIES, kind(group = "مسلسلات تركية"))
    @Test fun groupArabicMoviesIsMovie() = assertEquals(Kind.MOVIE, kind(group = "أفلام"))
    @Test fun groupArabicChannelIsLive() = assertEquals(Kind.LIVE, kind(group = "قنوات إخبارية"))
    @Test fun groupMovieChannelPrefersLive() = assertEquals(Kind.LIVE, kind(group = "Movie Channel"))
    @Test fun urlBeatsGroup() = assertEquals(Kind.LIVE, kind(url = "http://example.com/live/1.ts", group = "Movies"))

    // ---- name hints ----
    @Test fun nameSeasonEpisodeIsSeries() = assertEquals(Kind.SERIES, kind(name = "Show Name S01E03"))
    @Test fun nameMoviesTop100IsMovie() = assertEquals(Kind.MOVIE, kind(name = "Top 100 Movies"))

    // ---- stored type ----
    @Test fun storedTypeSeriesIsSeries() = assertEquals(Kind.SERIES, kind(type = "series"))
    @Test fun storedKindLiveIsLive() = assertEquals(Kind.LIVE, kind(kindField = "live"))

    // ---- extension fallback ----
    @Test fun mp4ExtensionIsMovie() = assertEquals(Kind.MOVIE, kind(url = "http://x.com/video.mp4"))
    @Test fun m3u8ExtensionAloneIsLive() = assertEquals(Kind.LIVE, kind(url = "http://x.com/stream.m3u8"))

    @Test fun unknownEverythingIsUnknown() = assertEquals(Kind.UNKNOWN, kind())

    // ---- HLS playlists are live channels even when group says Movies/Series ----
    @Test fun hlsMovieGroupIsLive() = assertEquals(Kind.LIVE, kind(url = "http://x.com/4ever/playlist.m3u8", group = "Movies"))
    @Test fun hlsSeriesGroupIsLive() = assertEquals(Kind.LIVE, kind(url = "http://x.com/drama/index.m3u8", group = "Series"))
    @Test fun hlsExplicitVodTypeWins() = assertEquals(Kind.MOVIE, kind(url = "http://x.com/vod.m3u8", type = "movie"))
    @Test fun progressiveMovieStillMovie() = assertEquals(Kind.MOVIE, kind(url = "http://x.com/f.mp4", group = "Movies"))

    // ---- query-param cache stays bounded and correct under many distinct URLs ----
    @Test fun queryCacheBoundedAndStable() {
        repeat(3000) { i -> Classification.getUrlExtension("http://x.com/v$i.m3u8") }
        assertEquals("m3u8", Classification.getUrlExtension("http://x.com/v0.m3u8"))
        assertEquals("mp4", Classification.getUrlExtension("http://x.com/v2999.mp4".replace("m3u8", "mp4")))
    }
}
