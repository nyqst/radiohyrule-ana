package com.radiohyrule.android.injection;

import com.radiohyrule.android.BuildConfig;
import com.radiohyrule.android.api.NowPlayingApi;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
public class ApiModule {

    private static final String BASE_URL = "https://radiohyrule.com";

    @Provides public static Retrofit providesRestAdapter() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (BuildConfig.DEBUG) {
            //gonna need to switch to injection if this gets any more complex
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(interceptor);
        }

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(builder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    @Provides public static NowPlayingApi providesNowPlayingService(Retrofit retrofit){
        return retrofit.create(NowPlayingApi.class);
    }
}
