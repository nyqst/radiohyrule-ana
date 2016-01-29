package com.radiohyrule.android.songinfo;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import com.radiohyrule.android.api.NowPlayingService;

import java.util.Comparator;
import java.util.Date;
import java.util.TreeSet;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.GsonConverterFactory;
import retrofit2.Response;
import retrofit2.Retrofit;

/*
 * Much nicer song info queue with nice retrofit callbacks instead of scary threading
 * Fair warning: This whole thing may get nuked if we can switch to better metadata management
 */
public class SongInfoQueue {

    public interface QueueObserver {
        void onNewPendingSong(SongInfo song);
    }

    protected static final String LOG_TAG = SongInfoQueue.class.getSimpleName();

    Handler handler;

    NowPlayingService nowPlayingService;
    TreeSet<SongInfo> songInfoSet;

    protected QueueObserver observer;
    @Nullable private Call<SongInfo> songInfoCall;
    private int retryCount;

    public SongInfoQueue() {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(NowPlayingService.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        nowPlayingService = retrofit.create(NowPlayingService.class);
        handler = new Handler(Looper.getMainLooper());

        songInfoSet = new TreeSet<>(new Comparator<SongInfo>() {
            @Override
            public int compare(SongInfo a, SongInfo b) {
                if (a == null || b == null) {
                    return -1;
                }
                if (a.songId == b.songId) {
                    return 0;
                } else {
                    return (int) (a.timeStarted - b.timeStarted); //first = lowest = started first
                }
            }
        });
    }


    private void onSongFetched(SongInfo songInfo) {
        Log.v(LOG_TAG, "Fetched Song: " + songInfo.title);
        boolean isNew = songInfoSet.add(songInfo);
        if (isNew && observer != null) {
            observer.onNewPendingSong(songInfo);
        }

        double waitTime; //seconds
        if (songInfo.songId > 0) {
            retryCount = 0;
            long songTimeElapsed = System.currentTimeMillis() / 1000 - songInfo.timeStarted;
            waitTime = Math.max(0, songInfo.duration - songTimeElapsed);
            waitTime += 1 + Math.random() * 2; //add 1..3 seconds
        } else { //advertisement, or livestream, or something? Exp Backoff
            waitTime = getExpBackoffWaitTime();
        }

        fetchAfterDelay(waitTime);
    }

    private void fetchAfterDelay(double waitTimeSeconds) {
        Runnable fetchNextSongRunnable = new Runnable() {
            @Override
            public void run() {
                fetchSongInfo(true);
            }
        };
        handler.postDelayed(fetchNextSongRunnable, (long) (waitTimeSeconds * 1000));
        Log.v(LOG_TAG, "Fetching new song in " + waitTimeSeconds + " seconds");
    }

    //Get exponentially-backed-off wait time
    private double getExpBackoffWaitTime() {
        double waitTime;
        double backoffMagnitude = Math.floor(Math.random() * ++retryCount);
        waitTime = ((long) Math.pow(2, backoffMagnitude) * 2);
        return waitTime;
    }


    public void setObserver(QueueObserver observer) {
        this.observer = observer;
    }

    public SongInfo getCurrentSong() {
        if (songInfoSet.isEmpty()) {
            fetchSongInfo(false);
            return null;
        }
        return songInfoSet.first();
    }

    public SongInfo moveToNextSong() {
        if (songInfoSet.isEmpty()) {
            fetchSongInfo(false);
            return null;
        }
        songInfoSet.remove(songInfoSet.first());
        return getCurrentSong();
    }

    public void onPlayerConnectingToStream() {
        fetchSongInfo(true);
    }

    public void onPlayerStopRequested() {
        reset();
    }

    private void reset() {
        if (songInfoCall != null) {
            songInfoCall.cancel();
            songInfoCall = null;
        }
        handler.removeCallbacksAndMessages(null);
        songInfoSet.clear();
    }

    private void fetchSongInfo(boolean cancelPending) {
        if (songInfoCall != null && !songInfoCall.isCanceled()) {
            //there is a request running
            if (cancelPending) {
                songInfoCall.cancel();
            } else {
                return;
            }
        }

        final Call<SongInfo> localCall = nowPlayingService.nowPlaying();
        songInfoCall = localCall;

        localCall.enqueue(new Callback<SongInfo>() {
            @Override
            public void onResponse(Response<SongInfo> response) {
                if (!localCall.isCanceled()) {
                    if (response.isSuccess()) {
                        SongInfo songInfo = response.body();
                        Date date = response.headers().getDate("Date");
                        songInfo.timeStamp = date != null ? date.getTime() / 1000 : System.currentTimeMillis() / 1000;
                        songInfo.setTimeElapsedAtStart(songInfo.timeStamp - songInfo.timeStarted);
                        if(date != null) Log.v(LOG_TAG, "Time offset: " + (System.currentTimeMillis() - date.getTime()));
                        onSongFetched(songInfo);
                    } else {
                        Log.e(LOG_TAG, "HTTP Error Fetching SongInfo: " + response.code() + " - " + response.message());
                        //if this is anything other than 5xx, this is probably unrecoverable.
                        if (response.code() >= 500) {
                            fetchAfterDelay(getExpBackoffWaitTime());
                        } //Todo notify user of unrecoverable failure?
                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.w(LOG_TAG, "Error Fetching SongInfo: ", t);
                fetchAfterDelay(getExpBackoffWaitTime());
            }
        });
    }
}
