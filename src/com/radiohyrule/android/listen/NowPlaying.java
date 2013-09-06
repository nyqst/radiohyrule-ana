package com.radiohyrule.android.listen;

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


        public Long getTimeStarted() {
            return timeStarted;
        }
        public long getTimeStartedValue() {
            return timeStarted == null ? 0 : timeStarted;
        }
        public void setTimeStarted(Long timeStarted) {
            this.timeStarted = timeStarted;
        }

        public Long getNumListeners() {
            return numListeners;
        }
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
        public void addArtist(String artist) {
            this.artists.add(artist);
        }
        public void clearArtists() {
            this.artists.clear();
        }

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
        public void setDuration(Double duration) {
            this.duration = duration;
        }
    }
}
