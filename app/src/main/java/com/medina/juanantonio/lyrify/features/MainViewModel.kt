package com.medina.juanantonio.lyrify.features

import android.view.KeyEvent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.spotify.sdk.android.auth.AuthorizationResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {
    val dispatchKeyEvent = MutableLiveData<KeyEvent>()
    val authorizationResponse = MutableLiveData<AuthorizationResponse>()
}