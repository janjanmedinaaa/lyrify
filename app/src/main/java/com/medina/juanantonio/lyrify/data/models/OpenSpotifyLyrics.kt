package com.medina.juanantonio.lyrify.data.models

data class OpenSpotifyLyrics(
    val error: Boolean,
    val syncType: String?,
    val lines: ArrayList<Lines>
) {

    class Lines(
        val startTimeMs: String,
        val words: String
    )

    val isLyricsSynced: Boolean
        get() = syncType == "LINE_SYNCED"
}