package com.example.testoauth.networking.profile

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header

interface OAuthProfileService {

    class BaseUrl {
        companion object {
            const val FETCH_PROFILE_BASE_URL = "https://oauth-account-noneu.truecaller.com/"
        }
    }

    class Endpoints {
        companion object {
            const val FETCH_PROFILE = "v1/userinfo"
        }
    }

    class Headers {
        companion object {
            const val TOKEN = "Authorization"
        }
    }

    @GET(Endpoints.FETCH_PROFILE)
    fun fetchProfile(
        @Header(Headers.TOKEN) accessToken: String
    ): Call<ResponseBody>
}