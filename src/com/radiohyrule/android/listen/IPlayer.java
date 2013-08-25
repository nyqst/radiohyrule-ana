package com.radiohyrule.android.listen;

public interface IPlayer {
    public boolean isPlaying();

    public void play();

    public void stop();

    public void togglePlaying();
}
