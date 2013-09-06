package com.radiohyrule.android.listen.player;

import com.radiohyrule.android.listen.NowPlaying;

public interface IPlayer {
    public boolean isPlaying();

    public void play();
    public void stop();
    public boolean togglePlaying();

    public NowPlaying.SongInfo getCurrentSong();

    public void setPlayerObserver(IPlayerObserver observer);
    public void removePlayerObserver(IPlayerObserver observer);

    public interface IPlayerObserver {
        public void onPlaybackStateChanged(boolean isPlaying);
        public void onCurrentSongChanged(NowPlaying.SongInfo song);
    }
}
