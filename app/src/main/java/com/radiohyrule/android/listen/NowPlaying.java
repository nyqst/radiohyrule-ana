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

}
