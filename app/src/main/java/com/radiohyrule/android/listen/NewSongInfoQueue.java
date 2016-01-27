package com.radiohyrule.android.listen;

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
import retrofit2.Response;
import retrofit2.Retrofit;

/*
 * Much nicer song info queue with nice retrofit callbacks instead of scary threading
 */
public class NewSongInfoQueue implements SongInfoQueue{

    protected static final String LOG_TAG = NewSongInfoQueue.class.getSimpleName();

    Handler handler;

    NowPlayingService nowPlayingService;
    TreeSet<SongInfo> songInfoSet;

    protected QueueObserver observer;
    @Nullable private Call<SongInfo> songInfoCall;
    private int retryCount;

    public NewSongInfoQueue(){

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(NowPlayingService.BASE_URL)
                .build();

        nowPlayingService = retrofit.create(NowPlayingService.class);
        handler = new Handler(Looper.getMainLooper());

        songInfoSet = new TreeSet<>(new Comparator<SongInfo>() {
            @Override
            public int compare(SongInfo a, SongInfo b) {
                if(a == null || b == null){
                    return -1;
                }
                if(a.songId.equals(b.songId)) { return 0; }
                else {
                    return (int) (a.timeStarted - b.timeStarted); //first = lowest = started first
                }
            }
        });
    }


    private void onSongFetched(SongInfo songInfo) {
        boolean isNew = songInfoSet.add(songInfo);
        if (isNew && observer != null) observer.onNewPendingSong(songInfo);


        double waitTime; //seconds
        if(songInfo.duration != null) {
            long songTimeElapsed = System.currentTimeMillis()/1000 - songInfo.timeStarted;
            waitTime = songInfo.duration - songTimeElapsed;
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
                fetchSongInfo();
            }
        };
        handler.postDelayed(fetchNextSongRunnable, (long) (waitTimeSeconds*1000));
        Log.v(LOG_TAG, "Fetching new song in " + waitTimeSeconds + " seconds");
    }

    //Get exponentially-backed-off wait time
    private double getExpBackoffWaitTime() {
        double waitTime;
        double backoffMagnitude = Math.floor(Math.random() * ++retryCount);
        waitTime = ((long) Math.pow(2, backoffMagnitude) * 2);
        return waitTime;
    }


    @Override
    public void setObserver(QueueObserver observer) {
        this.observer = observer;
    }

    @Override
    public SongInfo getCurrentSong() {
        SongInfo song = songInfoSet.first();
        if(song == null){
            fetchSongInfo();
        }
        return song;
    }

    @Override
    public SongInfo moveToNextSong() {
        songInfoSet.remove(songInfoSet.first());
        return getCurrentSong();
    }

    @Override
    public void onPlayerConnectingToStream(boolean resetConnection) {
        fetchSongInfo();
    }

    @Override
    public void onPlayerStopRequested() {
        reset();
    }

    private void reset(){
        if(songInfoCall != null) {
            songInfoCall.cancel();
            songInfoCall = null;
        }
        handler.removeCallbacksAndMessages(null);
        songInfoSet.clear();
    }

    private void fetchSongInfo(){
        if(songInfoCall != null) {
            songInfoCall.cancel();
        }

        final Call<SongInfo> localCall = nowPlayingService.nowPlaying();
        songInfoCall = localCall;

        localCall.enqueue(new Callback<SongInfo>() {
            @Override
            public void onResponse(Response<SongInfo> response) {
                if(!localCall.isCanceled()) {
                    if(response.isSuccess()) {
                        retryCount = 0;
                        SongInfo songInfo = response.body();
                        Date date = response.headers().getDate("Date");
                        songInfo.timeStamp = date != null ? date.getTime()/1000 : System.currentTimeMillis()/1000;
                        onSongFetched(songInfo);
                    } else {
                        Log.e(LOG_TAG, "HTTP Error Fetching SongInfo: " + response.code() + " - " + response.message());
                        //if this is anything other than 5xx, this is probably unrecoverable.
                        if(response.code() >= 500) {
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
