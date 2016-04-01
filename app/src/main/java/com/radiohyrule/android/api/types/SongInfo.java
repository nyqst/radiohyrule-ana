package com.radiohyrule.android.api.types;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class SongInfo {
    @SerializedName("title")
    public final String title;

    @SerializedName("song_url")
    public final String songUrl;

    @SerializedName("song_nid")
    public final int songId;

    @SerializedName("artist")
    public final ArrayList<String> artists;

    @SerializedName("artist_url")
    public final ArrayList<String> artistsUrl;

    @SerializedName("album")
    @Nullable
    public final String album;

    @SerializedName("album_url")
    @Nullable
    public final String albumUrl;

    @SerializedName("albumcover")
    @Nullable
    public final String albumCover;

    @SerializedName("started")
    public final long timeStarted; //seconds

    @SerializedName("duration")
    public final double duration; //seconds, or null for special things?

    @SerializedName("source")
    public final String source;

    @SerializedName("listeners")
    public final int numListeners; //this really ought to be a separate api call

    @SerializedName("request_username")
    @Nullable
    public final String requestUsername;

    @SerializedName("request_url")
    @Nullable
    public final String requestUrl;

    public SongInfo() {
        //assign some default values so we don't NPE all the time
        songId = -1;
        duration = -1;

        title = songUrl = album = albumUrl = albumCover = source = requestUsername = requestUrl = null;
        timeStarted = 0;
        numListeners = 0;

        artists = new ArrayList<>();
        artistsUrl = new ArrayList<>();
    }

    public long getEndTime(){
        return (long) (timeStarted + duration);
    }
}
