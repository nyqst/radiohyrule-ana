package com.radiohyrule.android.listen.player;

public interface IPlayer {
    public boolean isPlaying();

    public void play();
    public void stop();
    public void togglePlaying();

    public void setPlayerObserver(IPlayerObserver observer);
    public void removePlayerObserver(IPlayerObserver observer);

    public interface IPlayerObserver {
        public void onPlaybackStateChanged(boolean isPlaying);
    }
}
