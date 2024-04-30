package com.example.testoauth.networking;

import androidx.annotation.NonNull;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitAdapter {

    public static <T> T createService(@NonNull final String baseUrl, @NonNull Class<T> serviceClass) {
        final Retrofit.Builder retrofitBuilder = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create());

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        builder.addInterceptor(loggingInterceptor);
        final OkHttpClient okHttpClient = builder.build();
        retrofitBuilder.client(okHttpClient);

        return retrofitBuilder.build().create(serviceClass);
    }
}
