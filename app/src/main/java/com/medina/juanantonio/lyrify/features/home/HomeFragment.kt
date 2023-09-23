package com.medina.juanantonio.lyrify.features.home

import android.content.Context
import android.content.res.Resources
import android.media.AudioManager
import android.media.AudioManager.FLAG_SHOW_UI
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import coil.load
import com.medina.juanantonio.lyrify.R
import com.medina.juanantonio.lyrify.common.utils.StackLayoutManager
import com.medina.juanantonio.lyrify.common.utils.autoCleared
import com.medina.juanantonio.lyrify.common.utils.observeEvent
import com.medina.juanantonio.lyrify.data.adapters.Lyric
import com.medina.juanantonio.lyrify.data.adapters.LyricsAdapter
import com.medina.juanantonio.lyrify.data.models.SpotifyCurrentTrack
import com.medina.juanantonio.lyrify.databinding.FragmentHomeBinding
import com.medina.juanantonio.lyrify.features.MainViewModel
import com.spotify.sdk.android.auth.AuthorizationResponse
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.absoluteValue

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var binding: FragmentHomeBinding by autoCleared()
    private val viewModel: HomeViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    private val stackLayoutManager = StackLayoutManager(
        horizontalLayout = false,
        layoutInterpolator = LinearInterpolator(),
        maxViews = 4,
        viewTransformer = StackLayoutManager.ScaleTransformer::transform
    )
    private lateinit var lyricsAdapter: LyricsAdapter
    private lateinit var audioManager: AudioManager

    private val heightPixels: Int
        get() = Resources.getSystem().displayMetrics.heightPixels

    private var startingVolume = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_home,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lyricsAdapter = LyricsAdapter()
        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager

        binding.playerTouchView.setOnTouchEvent {
            when (it) {
                MotionEvent.ACTION_DOWN -> {
                    startingVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                }
            }
        }

        binding.playerTouchView.setOnPlayerSwiped { _, _, _, yChange ->
            try {
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val isLoweringVolume = yChange < 0
                val changePercentage = yChange.absoluteValue / heightPixels
                val volumeToBeAdded = when {
                    changePercentage >= 1 -> maxVolume
                    changePercentage <= 0 -> 0
                    else -> (maxVolume * changePercentage).toInt()
                }

                if (volumeToBeAdded == 0) return@setOnPlayerSwiped
                val newVolume =
                    if (isLoweringVolume) startingVolume - volumeToBeAdded
                    else startingVolume + volumeToBeAdded

                if ((0..maxVolume).contains(newVolume))
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, FLAG_SHOW_UI)
            } catch (e: Exception) {
                Log.d("HomeFragment", "${e.message}")
            }
        }

        binding.playerTouchView.setOnPlayerClicked {
            if (viewModel.spotifyPlaying.value == true) {
                viewModel.updateControl(it)
            } else {
                viewModel.requestUserCurrentTrack(
                    activity = requireActivity(),
                    authenticate = true
                ) { currentTrack ->
                    updateSpotifyLyrics(currentTrack)
                }
            }
        }

        binding.recyclerViewLyrics.apply {
            layoutManager = stackLayoutManager
            adapter = lyricsAdapter
        }

        binding.imageViewRefresh.setOnClickListener {
            viewModel.refresh()
        }

        listenToVM()
        listenToActivityVM()
    }

    override fun onResume() {
        super.onResume()
        viewModel.requestUserCurrentTrack(
            activity = requireActivity(),
            authenticate = false
        ) { currentTrack ->
            updateSpotifyLyrics(currentTrack)
        }
    }

    private fun listenToVM() {
        viewModel.songLyrics.observe(viewLifecycleOwner) { lyrics ->
            if (lyrics.isEmpty()) {
                lyricsAdapter.setLyrics(
                    arrayListOf(
                        Lyric(
                            line = getString(R.string.lyrics_not_available),
                            startTimeMs = 0
                        )
                    )
                )
            } else lyricsAdapter.setLyrics(lyrics)

            stackLayoutManager.scrollToPosition(0f, animated = false)
        }

        viewModel.currentTrack.observe(viewLifecycleOwner) {
            binding.imageViewAlbum.load(it.albumImageUrl)

            if (viewModel.isLyricsInSync.value == false) return@observe
            val nearestPreviousLine = lyricsAdapter.getNearestLineFromStartTime(it.playProgress)
            stackLayoutManager.scrollToPosition(nearestPreviousLine)
        }

        viewModel.requestUserCurrentTrack.observeEvent(viewLifecycleOwner) {
            Timer().schedule(500) {
                viewModel.requestUserCurrentTrack(
                    activity = requireActivity(),
                    authenticate = false
                ) {
                    updateSpotifyLyrics(it)
                }
                viewModel.isSpotifyRequestPending = false
            }
        }
    }

    private fun listenToActivityVM() {
        mainViewModel.dispatchKeyEvent.observe(viewLifecycleOwner) {
            if (viewModel.isLyricsInSync.value == true) return@observe
            when (it.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    val previousItem = stackLayoutManager.currentItem - 1
                    if (previousItem < 0) return@observe

                    stackLayoutManager.scrollToPosition(previousItem)
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    val nextItem = stackLayoutManager.currentItem + 1
                    val lyricsMaxIndex = viewModel.songLyrics.value?.size?.minus(1) ?: -1
                    if (nextItem > lyricsMaxIndex) return@observe

                    stackLayoutManager.scrollToPosition(nextItem)
                }
            }
        }

        mainViewModel.authorizationResponse.observe(viewLifecycleOwner) {
            when (it.type) {
                AuthorizationResponse.Type.TOKEN -> {
                    viewModel.saveAccessTokenFromAuthentication(it.accessToken)
                }
                AuthorizationResponse.Type.CODE -> {
                    viewModel.requestAccessToken(it.code)
                }
                else -> Unit
            }
        }
    }

    private fun updateSpotifyLyrics(
        currentTrack: SpotifyCurrentTrack?
    ) {
        viewModel.viewModelScope.launch(Dispatchers.Main) {
            if (currentTrack == null) {
                viewModel.songLyrics.value = arrayListOf()
                viewModel.currentSongTitle = ""
            } else {
                viewModel.getSongLyrics(currentTrack)
                viewModel.currentTrack.value = currentTrack
            }

            viewModel.spotifyPlaying.value = currentTrack?.item != null
        }
    }
}