package com.gomizplayer.iptv.core.epg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XmltvParserTest {

    private val xml = """<?xml version="1.0" encoding="UTF-8"?>
    <tv>
      <channel id="aje"><display-name>Al Jazeera</display-name></channel>
      <programme start="20261117120000 +0100" stop="20261117130000 +0100" channel="aje">
        <title lang="en">News &amp; Politics</title>
        <desc>Live coverage &lt;b&gt;today&lt;/b&gt;</desc>
        <category>news</category>
      </programme>
      <programme start="20261117130000 +0100" stop="20261117140000 +0100" channel="aje">
        <title>Next Show</title>
      </programme>
    </tv>"""

    @Test fun parsesProgrammes() {
        val programmes = Xmltv.parseEpg(xml)
        assertEquals(2, programmes.size)
        val first = programmes[0]
        assertEquals("aje", first.channel)
        assertEquals("News & Politics", first.title)
        assertEquals("Live coverage today", first.desc)
        assertEquals("news", first.category)
        assertEquals(Xmltv.parseXmltvTime("20261117120000 +0100"), first.start)
        assertEquals(Xmltv.parseXmltvTime("20261117130000 +0100"), first.stop)
    }

    @Test fun parseXmltvTimeHandlesOffsets() {
        // 2026-11-17 12:00:00 at +0100 == 11:00:00Z
        val ts = Xmltv.parseXmltvTime("20261117120000 +0100")!!
        val expected = java.time.ZonedDateTime.of(2026, 11, 17, 11, 0, 0, 0, java.time.ZoneOffset.UTC)
            .toInstant().toEpochMilli()
        assertEquals(expected, ts)
    }

    @Test fun parseXmltvTimeRejectsGarbage() {
        assertNull(Xmltv.parseXmltvTime("not a date"))
        assertNull(Xmltv.parseXmltvTime(null))
    }

    @Test fun entitiesDecoded() {
        val programmes = Xmltv.parseEpg(xml)
        assertTrue(programmes[0].title.contains("&"))
    }
}
