package com.radiohyrule.android.listen;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class NowPlaying {
    private Long time;
    private SongInfo song = new SongInfo();

    public Long getTime() {
        return time;
    }
    public long getTimeValue() {
        return time == null ? 0 : time;
    }
    public void setTime(Long time) {
        this.time = time;
    }

    public SongInfo getSong() {
        return song;
    }

    public static class SongInfo {
        private Long timeStarted;
        private Long numListeners;

        private String requestUsername;
        private String requestUrl;

        private String title;
        private List<String> artists = new LinkedList<String>();
        private String album;
        private String albumCover;
        private String songUrl;
        private Double duration;

        private Calendar timeStartedLocal;
        private long timeElapsedAtStart;


        public Long getTimeStarted() {
            return timeStarted;
        }
        public long getTimeStartedValue() { return timeStarted == null ? 0 : timeStarted; }
        public void setTimeStarted(Long timeStarted) {
            this.timeStarted = timeStarted;
        }

        public Long getNumListeners() {
            return numListeners;
        }
        public long getNumListenersValue() { return numListeners == null ? 0 : numListeners; }
        public void setNumListeners(Long numListeners) {
            this.numListeners = numListeners;
        }

        public String getRequestUsername() {
            return requestUsername;
        }
        public void setRequestUsername(String requestUsername) {
            this.requestUsername = requestUsername;
        }

        public String getRequestUrl() {
            return requestUrl;
        }
        public void setRequestUrl(String requestUrl) {
            this.requestUrl = requestUrl;
        }

        public String getTitle() {
            return title;
        }
        public void setTitle(String title) {
            this.title = title;
        }

        public Iterable<String> getArtists() {
            return artists;
        }
        public void addArtist(String artist) { this.artists.add(artist); }
        public void clearArtists() { this.artists.clear(); }

        public String getAlbum() {
            return album;
        }
        public void setAlbum(String album) {
            this.album = album;
        }

        public String getAlbumCover() {
            return albumCover;
        }
        public void setAlbumCover(String albumCover) {
            this.albumCover = albumCover;
        }

        public String getSongUrl() {
            return songUrl;
        }
        public void setSongUrl(String songUrl) {
            this.songUrl = songUrl;
        }

        public Double getDuration() {
            return duration;
        }
        public Double getDurationValue() { return duration == null ? 0.0 : duration; }
        public void setDuration(Double duration) {
            this.duration = duration;
        }

        public Calendar getTimeStartedLocal() { return timeStartedLocal; }
        public void setTimeStartedLocal(Calendar timeStartedLocal) { this.timeStartedLocal = timeStartedLocal; }

        public long getTimeElapsedAtStart() { return timeElapsedAtStart; }
        public void setTimeElapsedAtStart(long timeElapsedAtStart) { this.timeElapsedAtStart = timeElapsedAtStart; }

        // all in milliseconds
        public Long getEstimatedTimeUntilEnd(Calendar relativeToDate, long interruptionTime) {
            if (timeStartedLocal != null && duration != null && relativeToDate != null) {
                long timeRemainingAtStartLocal = ((int)(duration*1000.0)-((int)timeElapsedAtStart*1000)); // milliseconds
                long timeElapsedSinceStartLocal = relativeToDate.getTimeInMillis() - timeStartedLocal.getTimeInMillis();
                return timeRemainingAtStartLocal - timeElapsedSinceStartLocal + interruptionTime;
            } else {
                return null;
            }
        }
    }
}
