package com.radiohyrule.android.songinfo;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Calendar;

public class SongInfo {
    public final String title;

    @SerializedName("song_url")
    public final String songUrl;

    @SerializedName("song_nid")
    public final int songId;

    @SerializedName("artist")
    public final ArrayList<String> artists;

    @SerializedName("artist_url")
    public final ArrayList<String> artistsUrl;

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

    public final double duration; //seconds, or null for special things?

    public final String source;

    @SerializedName("listeners")
    public final int numListeners; //this really ought to be a separate api call

    @SerializedName("request_username")
    @Nullable
    public final String requestUsername;

    @SerializedName("request_url")
    @Nullable
    public final String requestUrl;

    transient public long timeStamp; //when data came from server. Seconds
    transient public long expectedLocalStartTime; //ms
    transient public long timeElapsedAtStart; //seconds

    public SongInfo() {
        //assign some default values so we don't NPE all the time
        songId = -1;
        duration = 5.0;

        title = songUrl = album = albumUrl = albumCover = source = requestUsername = requestUrl = null;
        timeStarted = 0;
        numListeners = 0;

        artists = new ArrayList<>();
        artistsUrl = new ArrayList<>();
    }

    //Methods and stuff

    public void setExpectedLocalStartTime(Calendar cal) {
        this.expectedLocalStartTime = cal.getTimeInMillis();
    }

    public void setTimeElapsedAtStart(long timeElapsedAtStart) {
        this.timeElapsedAtStart = timeElapsedAtStart;
    }

    // all in milliseconds
    public Long getEstimatedTimeUntilEnd(Long relativeTimeMs, long interruptionTime) {
        //todo I still don't 100% get this. Hopefully rendered irrelevant on switch to less buffer-y mediaplayer lib
        long timeRemainingAtStartLocal = ((long) (duration * 1000.0) - (timeElapsedAtStart * 1000)); // milliseconds
        long timeElapsedSinceStartLocal = relativeTimeMs - expectedLocalStartTime;
        return Math.max(0, timeRemainingAtStartLocal - timeElapsedSinceStartLocal + interruptionTime);
    }
}
