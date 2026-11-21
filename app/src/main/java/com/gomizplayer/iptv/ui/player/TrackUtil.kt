package com.gomizplayer.iptv.ui.player

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride

/** Cycles through the available audio tracks (e.g. multi-audio channels), like the Windows app. */
fun cycleAudioTrack(player: Player): String {
    val groups = player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
    val tracks = groups.flatMap { group ->
        (0 until group.length).map { index -> group to index }
    }
    if (tracks.size <= 1) return "Audio: only one track available"

    val selectedIndex = tracks.indexOfFirst { (group, index) -> group.isTrackSelected(index) }
    val next = tracks[(selectedIndex + 1) % tracks.size]
    val (group, index) = next

    val override = TrackSelectionOverride(group.mediaTrackGroup, listOf(index))
    player.trackSelectionParameters = player.trackSelectionParameters
        .buildUpon()
        .setOverrideForType(override)
        .build()

    val format = group.getTrackFormat(index)
    val label = format.language ?: format.label ?: "Track ${index + 1}"
    return "Audio: $label"
}
