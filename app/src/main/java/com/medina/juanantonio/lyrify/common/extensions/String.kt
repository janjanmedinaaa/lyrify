package com.medina.juanantonio.lyrify.common.extensions

import android.util.Base64
import com.google.gson.Gson
import com.medina.juanantonio.lyrify.data.models.Lyrics
import com.medina.juanantonio.lyrify.data.models.SpotifyAccessToken
import com.medina.juanantonio.lyrify.data.models.SpotifyCurrentTrack

fun String.toSpotifyCurrentTrack(): SpotifyCurrentTrack {
    return Gson().fromJson(this, SpotifyCurrentTrack::class.java)
}

fun String.toSpotifyAccessToken(): SpotifyAccessToken {
    return Gson().fromJson(this, SpotifyAccessToken::class.java)
}

fun String.toLyrics(): Lyrics {
    return Gson().fromJson(this, Lyrics::class.java)
}

fun String.toBase64(): String {
    return Base64.encodeToString(toByteArray(), Base64.NO_WRAP)
}