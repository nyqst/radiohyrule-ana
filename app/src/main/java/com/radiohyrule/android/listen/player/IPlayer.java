package com.radiohyrule.android.listen.player;

import com.radiohyrule.android.listen.SongInfo;

public interface IPlayer {
    public boolean isPlaying();

    public void play();
    public void stop();
    public boolean togglePlaying();

    public SongInfo getCurrentSong();

    public void setPlayerObserver(IPlayerObserver observer);
    public void removePlayerObserver(IPlayerObserver observer);

    public interface IPlayerObserver {
        public void onPlaybackStateChanged(boolean isPlaying);
        public void onCurrentSongChanged(SongInfo song);
    }
}
