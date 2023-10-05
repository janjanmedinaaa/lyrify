package com.medina.juanantonio.lyrify.common.extensions

import android.util.Base64
import com.google.gson.Gson
import com.medina.juanantonio.lyrify.data.models.*
import java.net.URLEncoder

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

fun String.toMusixMatchLyrics(): MusixMatchLyrics {
    return Gson().fromJson(this, MusixMatchLyrics::class.java)
}

// The builtin withParameters in Fuel HTTP keeps using + for spaces when URL Encoding,
// and keeps failing on the MusixMatch API
fun String.withParameters(vararg parameters: Pair<String, Any?>): String {
    return "$this?" + parameters
        .filterNot { (_, values) -> values == null }
        .flatMap { (key, values) ->
            // Deal with arrays
            ((values as? Iterable<*>)?.toList() ?: (values as? Array<*>)?.toList())?.let {
                val encodedKey = "${URLEncoder.encode(key, "UTF-8")}[]"
                it.map { value -> encodedKey to URLEncoder.encode(value.toString(), "UTF-8") }

                // Deal with regular
            } ?: listOf(URLEncoder.encode(key, "UTF-8") to URLEncoder.encode(values.toString(), "UTF-8").replace("+", "%20"))
        }
        .joinToString("&") { (key, value) -> if (value.isBlank()) key else "$key=$value" }
}
