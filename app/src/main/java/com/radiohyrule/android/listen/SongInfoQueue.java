package com.radiohyrule.android.listen;

/**
 * Probably-temporary interface to assist migration to a completely new class
 */
public interface SongInfoQueue {

    interface QueueObserver {
        void onNewPendingSong(SongInfo song);
    }

    void setObserver(QueueObserver observer);

    SongInfo getCurrentSong();

    SongInfo moveToNextSong();

    void onPlayerConnectingToStream(boolean resetConnection);

    void onPlayerStopRequested();
}
