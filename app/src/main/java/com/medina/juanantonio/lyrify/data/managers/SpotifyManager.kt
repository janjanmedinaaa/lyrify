package com.medina.juanantonio.lyrify.data.managers

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.httpPut
import com.github.kittinunf.result.Result
import com.medina.juanantonio.lyrify.BuildConfig
import com.medina.juanantonio.lyrify.common.extensions.*
import com.medina.juanantonio.lyrify.data.models.*
import kotlinx.coroutines.CompletableDeferred

class SpotifyManager : ISpotifyManager {

    override val clientId
        get() = BuildConfig.SPOTIFY_CLIENT_ID
    private val clientSecret
        get() = BuildConfig.SPOTIFY_CLIENT_SECRET

    companion object {
        const val REDIRECT_URL = "com.medina.juanantonio.lyrify://callback"

        private const val CURRENT_PLAYING_TRACK_URL =
            "https://api.spotify.com/v1/me/player/currently-playing"
        private const val REQUEST_ACCESS_TOKEN_URL =
            "https://accounts.spotify.com/api/token"

        private const val SEEK_TO_POSITION_URL =
            "https://api.spotify.com/v1/me/player/seek"
        private const val SKIP_TO_PREVIOUS_URL =
            "https://api.spotify.com/v1/me/player/previous"
        private const val SKIP_TO_NEXT_URL =
            "https://api.spotify.com/v1/me/player/next"
        private const val PAUSE_URL =
            "https://api.spotify.com/v1/me/player/pause"
        private const val PLAY_URL =
            "https://api.spotify.com/v1/me/player/play"

        private const val SPOTIFY_LYRICS_URL =
            "https://spotify-lyric-api.herokuapp.com"
        private const val MUSIX_MATCH_LYRICS_URL =
            "https://apic-desktop.musixmatch.com/ws/1.1/macro.subtitles.get"
    }

    override suspend fun requestAccessToken(code: String): SpotifyAccessToken? {
        val result = CompletableDeferred<SpotifyAccessToken?>()
        val request = REQUEST_ACCESS_TOKEN_URL.httpPost()
        request.header(
            mapOf(
                "Authorization" to "Basic ${("$clientId:$clientSecret").toBase64()}",
                "Content-Type" to "application/x-www-form-urlencoded"
            )
        )
        request.body("grant_type=authorization_code&code=$code&redirect_uri=$REDIRECT_URL")

        request.responseString { _, _, resultData ->
            when (resultData) {
                is Result.Success -> {
                    val accessToken = resultData.value.toSpotifyAccessToken()
                    result.complete(accessToken)
                }
                else -> {
                    result.complete(null)
                }
            }
        }.join()
        return result.await()
    }

    override suspend fun refreshAccessToken(refreshToken: String): SpotifyAccessToken? {
        val result = CompletableDeferred<SpotifyAccessToken?>()
        val request = REQUEST_ACCESS_TOKEN_URL.httpPost()
        request.header(
            mapOf(
                "Authorization" to "Basic ${("$clientId:$clientSecret").toBase64()}",
                "Content-Type" to "application/x-www-form-urlencoded"
            )
        )
        request.body("grant_type=refresh_token&refresh_token=$refreshToken")

        request.responseString { _, _, resultData ->
            when (resultData) {
                is Result.Success -> {
                    val accessToken = resultData.value.toSpotifyAccessToken()
                    result.complete(accessToken)
                }
                else -> {
                    result.complete(null)
                }
            }
        }.join()
        return result.await()
    }

    override suspend fun getUserCurrentTrack(token: String): Pair<Int, SpotifyCurrentTrack?> {
        val result = CompletableDeferred<Pair<Int, SpotifyCurrentTrack?>>()
        val request = CURRENT_PLAYING_TRACK_URL.httpGet()
        request.header(mapOf("Authorization" to "Bearer $token"))

        request.responseString { _, response, resultData ->
            when (resultData) {
                is Result.Success -> {
                    if (resultData.value.isNotEmpty()) {
                        val currentTrack = resultData.value.toSpotifyCurrentTrack()
                        result.complete(Pair(response.statusCode, currentTrack))
                    } else {
                        result.complete(Pair(response.statusCode, null))
                    }
                }
                else -> {
                    result.complete(Pair(response.statusCode, null))
                }
            }
        }.join()
        return result.await()
    }

    override suspend fun seekToPosition(token: String, positionMs: Int): Boolean {
        val result = CompletableDeferred<Boolean>()
        // httpPut(parameters) is not working. Added position_ms manually
        val request = "$SEEK_TO_POSITION_URL?position_ms=$positionMs".httpPut()
        request.header(mapOf("Authorization" to "Bearer $token"))

        request.responseString { _, _, resultData ->
            result.complete(resultData is Result.Success)
        }.join()
        return result.await()
    }

    override suspend fun skipToNext(token: String): Boolean {
        val result = CompletableDeferred<Boolean>()
        val request = SKIP_TO_NEXT_URL.httpPost()
        request.header(mapOf("Authorization" to "Bearer $token"))

        request.responseString { _, _, resultData ->
            result.complete(resultData is Result.Success)
        }.join()
        return result.await()
    }

    override suspend fun skipToPrevious(token: String): Boolean {
        val result = CompletableDeferred<Boolean>()
        val request = SKIP_TO_PREVIOUS_URL.httpPost()
        request.header(mapOf("Authorization" to "Bearer $token"))

        request.responseString { _, _, resultData ->
            result.complete(resultData is Result.Success)
        }.join()
        return result.await()
    }

    override suspend fun pause(token: String): Boolean {
        val result = CompletableDeferred<Boolean>()
        val request = PAUSE_URL.httpPut()
        request.header(mapOf("Authorization" to "Bearer $token"))

        request.responseString { _, _, resultData ->
            result.complete(resultData is Result.Success)
        }.join()
        return result.await()
    }

    override suspend fun play(token: String): Boolean {
        val result = CompletableDeferred<Boolean>()
        val request = PLAY_URL.httpPut()
        request.header(mapOf("Authorization" to "Bearer $token"))

        request.responseString { _, _, resultData ->
            result.complete(resultData is Result.Success)
        }.join()
        return result.await()
    }

    override suspend fun getTrackLyrics(track: SpotifyCurrentTrack): OpenSpotifyLyrics? {
        val spotifyLyrics = getTrackLyricsViaSpotifyLyrics(track.trackId)
        if (spotifyLyrics == null || !spotifyLyrics.isLyricsSynced) {
            return getTrackLyricsViaMusixMatch(
                title = track.songName,
                artist = track.item?.artists?.first()?.name ?: ""
            )?.toOpenSpotifyLyrics()
        }

        return spotifyLyrics
    }

    private suspend fun getTrackLyricsViaSpotifyLyrics(trackId: String): OpenSpotifyLyrics? {
        val result = CompletableDeferred<OpenSpotifyLyrics?>()
        val request = SPOTIFY_LYRICS_URL.httpGet(
            parameters = listOf(
                Pair("trackid", trackId)
            )
        )

        request.responseString { _, _, resultData ->
            when (resultData) {
                is Result.Success -> {
                    val openLyrics = resultData.value.toOpenSpotifyLyrics()
                    result.complete(openLyrics)
                }
                else -> {
                    result.complete(null)
                }
            }
        }.join()
        return result.await()
    }

    private suspend fun getTrackLyricsViaMusixMatch(
        title: String,
        artist: String
    ): MusixMatchLyrics? {
        val result = CompletableDeferred<MusixMatchLyrics?>()
        val request = MUSIX_MATCH_LYRICS_URL.withParameters(
            Pair("format", "json"),
            Pair("user_language", "en"),
            Pair("namespace", "lyrics_synced"),
            Pair("f_subtitle_length_max_deviation", 1),
            Pair("subtitle_format", "mxm"),
            Pair("app_id", "web-desktop-app-v1.0"),
            Pair("usertoken", "190511307254ae92ff84462c794732b84754b64a2f051121eff330"),
            Pair("q_track", title),
            Pair("q_artist", artist),
        ).httpGet()

        request.header(
            Pair("Cookie", "AWSELB=55578B011601B1EF8BC274C33F9043CA947F99DCFF0A80541772015CA2B39C35C0F9E1C932D31725A7310BCAEB0C37431E024E2B45320B7F2C84490C2C97351FDE34690157")
        )

        request.responseString { _, _, resultData ->
            when (resultData) {
                is Result.Success -> {
                    val musixMatchLyrics = resultData.value.toMusixMatchLyrics()
                    result.complete(musixMatchLyrics)
                }
                else -> {
                    result.complete(null)
                }
            }
        }.join()
        return result.await()
    }
}

interface ISpotifyManager {

    val clientId: String

    suspend fun requestAccessToken(code: String): SpotifyAccessToken?
    suspend fun refreshAccessToken(refreshToken: String): SpotifyAccessToken?
    suspend fun getUserCurrentTrack(token: String): Pair<Int, SpotifyCurrentTrack?>

    suspend fun seekToPosition(token: String, positionMs: Int): Boolean
    suspend fun skipToNext(token: String): Boolean
    suspend fun skipToPrevious(token: String): Boolean
    suspend fun pause(token: String): Boolean
    suspend fun play(token: String): Boolean

    suspend fun getTrackLyrics(track: SpotifyCurrentTrack): OpenSpotifyLyrics?
}
