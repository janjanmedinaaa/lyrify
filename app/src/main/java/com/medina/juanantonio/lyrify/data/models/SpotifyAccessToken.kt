package com.medina.juanantonio.lyrify.data.models

data class SpotifyAccessToken(
    val access_token: String,
    val token_type: String,
    val scope: String,
    val expires_in: String,
    val refresh_token: String?
)