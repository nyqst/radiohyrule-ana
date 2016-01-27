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
    public String songId;

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

    @Nullable public Double duration; //seconds, or null for special things?

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

    //Methods and stuff

    public Double getDuration() {
        return duration != null ? duration : 0;
    }

    public void setExpectedLocalStartTime(Calendar cal) { this.expectedLocalStartTime = cal.getTimeInMillis(); }

    public void setTimeElapsedAtStart(long timeElapsedAtStart) { this.timeElapsedAtStart = timeElapsedAtStart; }

    // all in milliseconds
    public Long getEstimatedTimeUntilEnd(Long relativeTime, long interruptionTime) {
        //todo I still don't 100% get this. Hopefully rendered irrelevant on switch to less buffer-y mediaplayer lib
        if (duration != null) {
            long timeRemainingAtStartLocal = ((long) (duration * 1000.0) - (timeElapsedAtStart * 1000)); // milliseconds
            long timeElapsedSinceStartLocal = relativeTime - expectedLocalStartTime;
            return timeRemainingAtStartLocal - timeElapsedSinceStartLocal + interruptionTime;
        } else {
            return null;
        }
    }
}
