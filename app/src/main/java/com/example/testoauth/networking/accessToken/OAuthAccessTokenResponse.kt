package com.example.testoauth.networking.accessToken

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class OAuthAccessTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    val expires_in: Int,
    val scope: String,
    val token_type: String
)