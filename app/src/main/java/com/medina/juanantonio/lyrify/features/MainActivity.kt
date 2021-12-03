package com.medina.juanantonio.lyrify.features

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_VOLUME_DOWN
import android.view.KeyEvent.KEYCODE_VOLUME_UP
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.medina.juanantonio.lyrify.data.managers.SpotifyManager
import com.medina.juanantonio.lyrify.databinding.ActivityMainBinding
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationResponse
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    // From External Browser
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.let {
            val response = AuthorizationResponse.fromUri(it)
            viewModel.authorizationResponse.value = response
        }
    }

    // From Fragment Dialog
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != SpotifyManager.REQUEST_CODE) return

        val response = AuthorizationClient.getResponse(resultCode, data)
        viewModel.authorizationResponse.value = response
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        return when (event?.keyCode) {
            KEYCODE_VOLUME_UP,
            KEYCODE_VOLUME_DOWN -> {
                viewModel.dispatchKeyEvent.value = event
                true
            }
            else -> super.dispatchKeyEvent(event)
        }
    }
}