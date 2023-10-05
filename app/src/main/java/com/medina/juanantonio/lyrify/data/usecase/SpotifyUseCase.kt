package com.medina.juanantonio.lyrify.data.usecase

import android.app.Activity
import com.medina.juanantonio.lyrify.common.Constants.PreferencesKey.SPOTIFY_ACCESS_TOKEN
import com.medina.juanantonio.lyrify.common.Constants.PreferencesKey.SPOTIFY_CODE
import com.medina.juanantonio.lyrify.common.Constants.PreferencesKey.SPOTIFY_REFRESH_TOKEN
import com.medina.juanantonio.lyrify.data.managers.IDataStoreManager
import com.medina.juanantonio.lyrify.data.managers.ISpotifyManager
import com.medina.juanantonio.lyrify.data.models.OpenSpotifyLyrics
import com.medina.juanantonio.lyrify.data.models.SpotifyCurrentTrack
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class SpotifyUseCase @Inject constructor(
    private val spotifyManager: ISpotifyManager,
    private val dataStoreManager: IDataStoreManager
) {

    companion object {
        const val REQUEST_CODE = 1337
        const val REDIRECT_URL = "com.medina.juanantonio.lyrify://callback"
    }

    fun authenticate(activity: Activity, externalBrowser: Boolean = true): String {
        return try {
            val state = Random.nextInt(100000, 999999).toString()
            val builder = AuthorizationRequest.Builder(
                spotifyManager.clientId,
                AuthorizationResponse.Type.CODE,
                REDIRECT_URL
            ).setScopes(arrayOf("user-read-playback-state", "user-modify-playback-state"))
                .setState(state)
            val request = builder.build()

            if (externalBrowser)
                AuthorizationClient.openLoginInBrowser(activity, request)
            else
                AuthorizationClient.openLoginActivity(activity, REQUEST_CODE, request)

            state
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun hasExistingUser(): Boolean {
        return dataStoreManager.getString(SPOTIFY_ACCESS_TOKEN).isNotEmpty()
    }

    suspend fun saveAccessToken(accessToken: String) {
        dataStoreManager.putString(SPOTIFY_ACCESS_TOKEN, accessToken)
    }

    suspend fun requestAccessToken(code: String): String {
        dataStoreManager.putString(SPOTIFY_CODE, code)
        val result = spotifyManager.requestAccessToken(code)
        result?.apply {
            saveAccessToken(access_token)
            dataStoreManager.putString(SPOTIFY_REFRESH_TOKEN, refresh_token ?: "")
        }

        return result?.access_token ?: ""
    }

    private suspend fun refreshAccessToken(): String {
        val refreshToken = dataStoreManager.getString(SPOTIFY_REFRESH_TOKEN)
        val result = spotifyManager.refreshAccessToken(refreshToken)
        result?.apply {
            saveAccessToken(access_token)
        }

        return result?.access_token ?: ""
    }

    suspend fun getCurrentPlayingTrack(): SpotifyCurrentTrack? {
        val token = dataStoreManager.getString(SPOTIFY_ACCESS_TOKEN)
        val (requestCode, currentTrack) = spotifyManager.getUserCurrentTrack(token)
        if (requestCode == 401) {
            refreshAccessToken()
            return getCurrentPlayingTrack()
        }

        return currentTrack
    }

    suspend fun getSongLyrics(track: SpotifyCurrentTrack): OpenSpotifyLyrics? {
        return spotifyManager.getTrackLyrics(track)
    }

    suspend fun seekToPosition(positionMs: Int) {
        val token = dataStoreManager.getString(SPOTIFY_ACCESS_TOKEN)
        spotifyManager.seekToPosition(token, positionMs)
    }

    suspend fun skipToNext() {
        val token = dataStoreManager.getString(SPOTIFY_ACCESS_TOKEN)
        spotifyManager.skipToNext(token)
    }

    suspend fun skipToPrevious() {
        val token = dataStoreManager.getString(SPOTIFY_ACCESS_TOKEN)
        spotifyManager.skipToPrevious(token)
    }

    suspend fun play() {
        val token = dataStoreManager.getString(SPOTIFY_ACCESS_TOKEN)
        spotifyManager.play(token)
    }

    suspend fun pause() {
        val token = dataStoreManager.getString(SPOTIFY_ACCESS_TOKEN)
        spotifyManager.pause(token)
    }
}