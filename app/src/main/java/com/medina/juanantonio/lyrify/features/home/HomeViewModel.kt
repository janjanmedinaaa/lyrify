package com.medina.juanantonio.lyrify.features.home

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medina.juanantonio.lyrify.common.utils.Event
import com.medina.juanantonio.lyrify.common.utils.toEvent
import com.medina.juanantonio.lyrify.common.views.PlayerTouchView
import com.medina.juanantonio.lyrify.data.adapters.Lyric
import com.medina.juanantonio.lyrify.data.adapters.LyricsAdapter
import com.medina.juanantonio.lyrify.data.models.SpotifyCurrentTrack
import com.medina.juanantonio.lyrify.data.usecase.SpotifyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val spotifyUseCase: SpotifyUseCase
) : ViewModel() {
    private var spotifyJob: Job? = null
    private var lyricsJob: Job? = null
    private var spotifyControlJob: Job? = null

    val spotifyLoading = MutableLiveData(false)
    val spotifyPlaying = MutableLiveData(false)
    val isLyricsInSync = MutableLiveData(false)

    private val _requestUserCurrentTrack = MutableLiveData<Event<Unit>>()
    val requestUserCurrentTrack: LiveData<Event<Unit>>
        get() = _requestUserCurrentTrack

    val songLyrics = MutableLiveData(arrayListOf<Lyric>())
    val hasLyrics = MediatorLiveData<Boolean>().apply {
        addSource(songLyrics) {
            this.value = it.isNotEmpty()
        }
    }

    var isSpotifyRequestPending = false
    val currentTrack = MutableLiveData<SpotifyCurrentTrack>()

    private var invalidTrackCounter = 0
    val resetDisplay: Boolean
        get() = invalidTrackCounter > 10

    var currentSongTitle = ""

    fun getSongLyrics(currentTrack: SpotifyCurrentTrack) {
        if (lyricsJob?.isActive == true) return
        if (currentTrack.songName == currentSongTitle) return
        currentSongTitle = currentTrack.songName
        lyricsJob = viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                spotifyLoading.value = true
            }

            val lyrics = spotifyUseCase.getSongLyrics(currentTrack.trackId)

            withContext(Dispatchers.Main) {
                isLyricsInSync.value = lyrics?.isLyricsSynced == true
                spotifyLoading.value = false
                songLyrics.value = LyricsAdapter.toLyricList(lyrics)
            }
        }
    }

    private fun requestUserCurrentTrack() {
        viewModelScope.launch(Dispatchers.Main) {
            _requestUserCurrentTrack.value = Unit.toEvent()
        }
    }

    fun requestUserCurrentTrack(
        activity: Activity,
        authenticate: Boolean,
        onCurrentTrack: (SpotifyCurrentTrack?) -> Unit
    ) {
        if (spotifyJob?.isActive == true) return
        spotifyJob = viewModelScope.launch(Dispatchers.IO) {
            val hasExistingUser = spotifyUseCase.hasExistingUser()
            if (!hasExistingUser) {
                if (!authenticate) return@launch
                spotifyUseCase.authenticate(activity)
                isSpotifyRequestPending = true
            } else {
                val currentTrack = spotifyUseCase.getCurrentPlayingTrack()
                requestUserCurrentTrack()

                if (currentTrack == null) invalidTrackCounter++
                else invalidTrackCounter = 0

                if (currentTrack != null || resetDisplay)
                    onCurrentTrack(currentTrack)
            }
        }
    }

    fun refresh() {
        spotifyJob?.cancel()
        currentSongTitle = ""
        requestUserCurrentTrack()
    }

    fun saveAccessTokenFromAuthentication(accessToken: String) {
        viewModelScope.launch(Dispatchers.IO) {
            spotifyUseCase.saveAccessToken(accessToken)
            if (isSpotifyRequestPending) requestUserCurrentTrack()
        }
    }

    fun requestAccessToken(code: String) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                spotifyLoading.value = true
            }

            spotifyUseCase.requestAccessToken(code)
            requestUserCurrentTrack()

            withContext(Dispatchers.Main) {
                spotifyLoading.value = false
            }
        }
    }

    fun updateControl(action: PlayerTouchView.PlayerAction) {
        if (spotifyControlJob?.isActive == true) return
        spotifyControlJob = viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                spotifyLoading.value = true
            }

            when (action) {
                PlayerTouchView.PlayerAction.SKIP_PREVIOUS -> {
                    if ((currentTrack.value?.playProgress ?: 0) < 5000) {
                        spotifyUseCase.skipToPrevious()
                    } else {
                        spotifyUseCase.seekToPosition(0)
                    }
                }
                PlayerTouchView.PlayerAction.SKIP_NEXT -> spotifyUseCase.skipToNext()
                PlayerTouchView.PlayerAction.PLAY_PAUSE -> {
                    if (currentTrack.value?.isMusicPlaying == true) {
                        spotifyUseCase.pause()
                    } else {
                        spotifyUseCase.play()
                    }
                }
            }

            withContext(Dispatchers.Main) {
                spotifyLoading.value = false
            }
        }
    }
}