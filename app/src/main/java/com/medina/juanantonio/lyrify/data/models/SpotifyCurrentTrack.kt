package com.medina.juanantonio.lyrify.data.models

data class SpotifyCurrentTrack(
    val item: Item?,
    val is_playing: Boolean
) {

    data class Item(
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
}