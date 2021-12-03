package com.medina.juanantonio.lyrify.features.home

import android.app.Activity
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medina.juanantonio.lyrify.common.Constants.PreferencesKey.SPOTIFY_ACCESS_TOKEN
import com.medina.juanantonio.lyrify.common.Constants.PreferencesKey.SPOTIFY_REFRESH_TOKEN
import com.medina.juanantonio.lyrify.common.utils.Event
import com.medina.juanantonio.lyrify.common.utils.toEvent
import com.medina.juanantonio.lyrify.data.managers.IDataStoreManager
import com.medina.juanantonio.lyrify.data.managers.ISpotifyManager
import com.medina.juanantonio.lyrify.data.managers.LyricsManager
import com.medina.juanantonio.lyrify.data.managers.SpotifyManager
import com.medina.juanantonio.lyrify.data.models.SpotifyCurrentTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import kotlin.concurrent.schedule

@HiltViewModel
class HomeViewModel @Inject constructor(
    private var spotifyManager: ISpotifyManager,
    private var dataStoreManager: IDataStoreManager
) : ViewModel() {
    private var spotifyJob: Job? = null
    private var lyricsJob: Job? = null
    private var spotifyControlJob: Job? = null

    val spotifyLoading = MutableLiveData(false)
    val spotifyPlaying = MutableLiveData(false)

    val spotifyCode = MutableLiveData<Event<String>>()
    val spotifyAccessToken = MutableLiveData<Event<String>>()
    val spotifyRefreshToken = MutableLiveData<Event<String>>()
    val songLyrics = MutableLiveData(arrayListOf<String>())

    var isSpotifyRequestPending = false
    val currentTrack = MutableLiveData<SpotifyCurrentTrack>()
    private var currentSongTitle = ""

    fun getSongLyrics(currentTrack: SpotifyCurrentTrack) {
        if (lyricsJob?.isActive == true) return
        if (currentTrack.songName == currentSongTitle) return
        currentSongTitle = currentTrack.songName
        lyricsJob = viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                spotifyLoading.value = true
            }

            val lyrics = LyricsManager.getSongLyrics(
                artist = currentTrack.artist,
                title = currentTrack.songName
            )

            withContext(Dispatchers.Main) {
                spotifyLoading.value = false
                songLyrics.value = LyricsManager.formatLyrics(lyrics.lyrics ?: "")
            }
        }
    }

    fun requestUserCurrentTrack(
        activity: Activity,
        authenticate: Boolean,
        onCurrentTrack: (SpotifyCurrentTrack?) -> Unit
    ) {
        if (spotifyJob?.isActive == true) return
        spotifyJob = viewModelScope.launch(Dispatchers.IO) {
            val token = dataStoreManager.getString(SPOTIFY_ACCESS_TOKEN)
            if (token.isEmpty()) {
                if (!authenticate) return@launch
                spotifyManager.authenticate(activity)
                isSpotifyRequestPending = true
            } else {
                val (requestCode, currentTrack) =
                    spotifyManager.getUserCurrentTrack(token)

                if (requestCode == 401) {
                    isSpotifyRequestPending = true
                    refreshAccessToken(dataStoreManager.getString(SPOTIFY_REFRESH_TOKEN))
                } else {
                    Timer().schedule(500) {
                        requestUserCurrentTrack(activity, authenticate, onCurrentTrack)
                    }
                }

                onCurrentTrack(currentTrack)
                Log.d(SpotifyManager.TAG, "$requestCode, ${currentTrack?.songName}")
            }
        }
    }

    suspend fun requestAccessToken(code: String) {
        withContext(Dispatchers.Main) {
            spotifyLoading.value = true
        }
        val result = spotifyManager.requestAccessToken(code)

        withContext(Dispatchers.Main) {
            spotifyAccessToken.value = result?.access_token?.toEvent()
            result?.refresh_token?.let {
                spotifyRefreshToken.value = it.toEvent()
            }
            spotifyLoading.value = false
        }
    }

    private suspend fun refreshAccessToken(refreshToken: String) {
        withContext(Dispatchers.Main) {
            spotifyLoading.value = true
        }
        val result = spotifyManager.refreshAccessToken(refreshToken)

        withContext(Dispatchers.Main) {
            spotifyAccessToken.value = result?.access_token?.toEvent()
            spotifyLoading.value = false
        }
    }

    fun skipToNext() {
        if (spotifyControlJob?.isActive == true) return
        spotifyControlJob = viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                spotifyLoading.value = true
            }

            val token = dataStoreManager.getString(SPOTIFY_ACCESS_TOKEN)
            spotifyManager.skipToNext(token)

            withContext(Dispatchers.Main) {
                spotifyLoading.value = false
            }
        }
    }

    fun skipToPrevious() {
        if (spotifyControlJob?.isActive == true) return
        spotifyControlJob = viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                spotifyLoading.value = true
            }

            val token = dataStoreManager.getString(SPOTIFY_ACCESS_TOKEN)
            spotifyManager.skipToPrevious(token)

            withContext(Dispatchers.Main) {
                spotifyLoading.value = false
            }
        }
    }

    fun play() {
        if (spotifyControlJob?.isActive == true) return
        spotifyControlJob = viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                spotifyLoading.value = true
            }

            val token = dataStoreManager.getString(SPOTIFY_ACCESS_TOKEN)
            spotifyManager.play(token)

            withContext(Dispatchers.Main) {
                spotifyLoading.value = false
            }
        }
    }

    fun pause() {
        if (spotifyControlJob?.isActive == true) return
        spotifyControlJob = viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                spotifyLoading.value = true
            }

            val token = dataStoreManager.getString(SPOTIFY_ACCESS_TOKEN)
            spotifyManager.pause(token)

            withContext(Dispatchers.Main) {
                spotifyLoading.value = false
            }
        }
    }
}