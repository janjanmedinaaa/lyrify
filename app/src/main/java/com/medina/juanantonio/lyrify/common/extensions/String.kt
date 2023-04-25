package com.medina.juanantonio.lyrify.common.extensions

import android.util.Base64
import com.google.gson.Gson
import com.medina.juanantonio.lyrify.data.models.*

fun String.toSpotifyCurrentTrack(): SpotifyCurrentTrack {
    return Gson().fromJson(this, SpotifyCurrentTrack::class.java)
}

fun String.toSpotifyAccessToken(): SpotifyAccessToken {
    return Gson().fromJson(this, SpotifyAccessToken::class.java)
}

fun String.toOpenSpotifyLyrics(): OpenSpotifyLyrics {
    return Gson().fromJson(this, OpenSpotifyLyrics::class.java)
}

fun String.toBase64(): String {
    return Base64.encodeToString(toByteArray(), Base64.NO_WRAP)
}