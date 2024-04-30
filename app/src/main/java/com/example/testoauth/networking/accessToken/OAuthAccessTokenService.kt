package com.example.testoauth.networking.accessToken

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface OAuthAccessTokenService {

    class BaseUrl {
        companion object {
            const val FETCH_ACCESS_TOKEN_BASE_URL = "https://oauth-account-noneu.truecaller.com/"
        }
    }

    class Endpoints {
        companion object {
            const val FETCH_ACCESS_TOKEN = "v1/token"
        }
    }

    @FormUrlEncoded
    @POST(Endpoints.FETCH_ACCESS_TOKEN)
    fun fetchAccessToken(
        @Field("grant_type") grantType: String,
        @Field("client_id") clientId: String,
        @Field("code") code: String,
        @Field("code_verifier") codeVerifier: String,
    ): Call<OAuthAccessTokenResponse>
}