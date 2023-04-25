package com.medina.juanantonio.lyrify.data.models

data class SpotifyCurrentTrack(
    val item: Item?,
    val is_playing: Boolean,
    val progress_ms: Int?
) {

    data class Item(
        val id: String,
        val name: String,
        val album: Album,
        val artists: List<Artist>,
    ) {

        data class Artist(
            val name: String
        )

        data class Album(
            val images: List<Image>,
            val name: String
        ) {

            data class Image(
                val height: Int,
                val url: String,
                val width: Int
            )
        }
    }

    val trackId: String
        get() = item?.id ?: ""

    val songName: String
        get() = item?.name ?: ""

    val albumImageUrl: String
        get() = item?.album?.images?.first()?.url ?: ""

    val artist: String
        get() {
            return item?.artists?.joinToString(separator = ", ") { it.name } ?: ""
        }

    val albumName: String
        get() = item?.album?.name ?: ""

    val playProgress: Int
        get() = progress_ms ?: 0

    val isMusicPlaying: Boolean
        get() = item != null && is_playing
}