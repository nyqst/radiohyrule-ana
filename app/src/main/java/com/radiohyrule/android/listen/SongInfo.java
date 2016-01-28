package com.radiohyrule.android.listen;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Calendar;

public class SongInfo {
    //todo Make most of these final once Old queue is dead

    public String title;

    @SerializedName("song_url")
    public String songUrl;

    @SerializedName("song_nid")
    public int songId;

    @SerializedName("artist")
    @Nullable
    public ArrayList<String> artists;

    @SerializedName("artist_url")
    @Nullable public ArrayList<String> artistsUrl;

    @Nullable public String album;

    @SerializedName("album_url")
    @Nullable public String albumUrl;

    @SerializedName("albumcover")
    @Nullable public String albumCover;

    @SerializedName("started")
    public long timeStarted; //seconds

    public double duration; //seconds, or null for special things?

    public String source;

    @SerializedName("listeners")
    public int numListeners; //this really ought to be a separate api call

    @SerializedName("request_username")
    @Nullable public String requestUsername;
    @SerializedName("request_url")
    @Nullable public String requestUrl;

    transient public long timeStamp; //when data came from server. Seconds
    transient public long expectedLocalStartTime;
    transient public long timeElapsedAtStart;

    public SongInfo(){
        //assign some default values so we don't NPE all the time
        songId = -1;
        duration = 5.0;
    }

    //Methods and stuff

    public void setExpectedLocalStartTime(Calendar cal) { this.expectedLocalStartTime = cal.getTimeInMillis(); }

    public void setTimeElapsedAtStart(long timeElapsedAtStart) { this.timeElapsedAtStart = timeElapsedAtStart; }

    // all in milliseconds
    public Long getEstimatedTimeUntilEnd(Long relativeTime, long interruptionTime) {
        //todo I still don't 100% get this. Hopefully rendered irrelevant on switch to less buffer-y mediaplayer lib
            long timeRemainingAtStartLocal = ((long) (duration * 1000.0) - (timeElapsedAtStart * 1000)); // milliseconds
            long timeElapsedSinceStartLocal = relativeTime - expectedLocalStartTime;
            return Math.max(0, timeRemainingAtStartLocal - timeElapsedSinceStartLocal + interruptionTime);
    }
}
