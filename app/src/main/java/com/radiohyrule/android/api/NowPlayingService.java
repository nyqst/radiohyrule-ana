package com.radiohyrule.android.api;

import com.radiohyrule.android.listen.SongInfo;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Service definition for Now Playing API.
 */
public interface NowPlayingService {
    String BASE_URL = "https://radiohyrule.com";

    @GET("nowplaying.json")
    Call<SongInfo> nowPlaying();
}
