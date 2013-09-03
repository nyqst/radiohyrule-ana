package com.radiohyrule.android.listen;

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

        private String title;
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

        public String getTitle() {
            return title;
        }
        public void setTitle(String title) {
            this.title = title;
        }

        public Double getDuration() {
            return duration;
        }
        public void setDuration(Double duration) {
            this.duration = duration;
        }
    }
}
