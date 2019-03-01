package com.radiohyrule.android.api;

import com.radiohyrule.android.api.types.SongInfo;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Service definition for Now Playing API.
 */
public interface NowPlayingApi {

    @GET("nowplaying.json")
    Call<SongInfo> nowPlaying();
}
