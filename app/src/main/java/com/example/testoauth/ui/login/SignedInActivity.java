package com.example.testoauth.ui.login;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import com.example.testoauth.R;
import com.example.testoauth.databinding.ActivitySignedInBinding;
import com.example.testoauth.networking.RetrofitAdapter;
import com.example.testoauth.networking.accessToken.OAuthAccessTokenResponse;
import com.example.testoauth.networking.accessToken.OAuthAccessTokenService;
import com.example.testoauth.networking.profile.OAuthProfileService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.truecaller.android.sdk.oAuth.TcOAuthData;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignedInActivity extends Activity {

    private OAuthAccessTokenService accessTokenService;
    private OAuthProfileService     profileService;
    private ActivitySignedInBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignedInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        final TcOAuthData oAuthData = getIntent().getParcelableExtra("data");
        final String requestedState = getIntent().getStringExtra("state");
        final String codeVerifier = getIntent().getStringExtra("cv");
        binding.accessTokenBtn.setOnClickListener(view -> fetchAccessToken(oAuthData, codeVerifier));
        binding.signedInTv.setText(String.format(getString(R.string.large_text),
                oAuthData.getAuthorizationCode(), codeVerifier,
                oAuthData.getState(), requestedState, oAuthData.getScopesGranted()));
        createRetrofitService();
    }

    private void createRetrofitService() {
        accessTokenService = RetrofitAdapter.createService(
                OAuthAccessTokenService.BaseUrl.FETCH_ACCESS_TOKEN_BASE_URL,
                OAuthAccessTokenService.class);
        profileService = RetrofitAdapter.createService(
                OAuthProfileService.BaseUrl.FETCH_PROFILE_BASE_URL,
                OAuthProfileService.class);
    }

    private void fetchAccessToken(final TcOAuthData oAuthData, final String codeVerifier) {
        setAccessTokenText("Fetching access token...");
        accessTokenService.fetchAccessToken(
                "authorization_code",
                getString(R.string.clientId),
                oAuthData.getAuthorizationCode(),
                codeVerifier
        ).enqueue(new Callback<OAuthAccessTokenResponse>() {
            @Override
            public void onResponse(@NotNull final Call<OAuthAccessTokenResponse> call, @NotNull final Response<OAuthAccessTokenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String accessToken = response.body().getAccessToken();
                    setAccessTokenText(String.format(getString(R.string.access_token), accessToken));
                    binding.profileBtn.setOnClickListener(view -> fetchProfile(accessToken));
                } else {
                    Toast.makeText(SignedInActivity.this, "Unable to fetch access token", Toast.LENGTH_SHORT).show();
                    setAccessTokenText("error: " + response.message());
                }
            }

            @Override
            public void onFailure(@NotNull final Call<OAuthAccessTokenResponse> call, @NotNull final Throwable t) {
                String msg = "Unable to fetch access token: " + t.getMessage();
                Toast.makeText(SignedInActivity.this, msg, Toast.LENGTH_SHORT).show();
                setAccessTokenText(msg);
            }
        });
    }

    private void fetchProfile(final String accessToken) {
        setProfileText("Fetching profile...");
        String bearerToken = String.format(getString(R.string.bearer_token), accessToken);
        profileService.fetchProfile(bearerToken).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NotNull final Call<ResponseBody> call, @NotNull final Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonElement jsonElement = new JsonParser().parse(response.body().string());
                        Gson gson = new GsonBuilder().setPrettyPrinting().create();
                        setProfileText(gson.toJson(jsonElement));
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(SignedInActivity.this, "Unable to fetch profile exception", Toast.LENGTH_SHORT).show();
                        setProfileText("exception: " + e.getMessage());
                    }
                } else {
                    Toast.makeText(SignedInActivity.this, "Unable to fetch profile", Toast.LENGTH_SHORT).show();
                    setProfileText("error: " + response.message());
                }
            }

            @Override
            public void onFailure(@NotNull final Call<ResponseBody> call, @NotNull final Throwable t) {
                String msg = "Unable to fetch profile: " + t.getMessage();
                Toast.makeText(SignedInActivity.this, msg, Toast.LENGTH_SHORT).show();
                setProfileText(msg);
            }
        });
    }

    private void setAccessTokenText(String text) {
        binding.accessTokenTv.setText(text);
    }

    private void setProfileText(String text) {
        binding.profileDetailsTv.setText(text);
    }

}
