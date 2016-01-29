package com.radiohyrule.android.player;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.radiohyrule.android.R;
import com.radiohyrule.android.activities.MainActivity;
import com.radiohyrule.android.songinfo.SongInfo;
import com.radiohyrule.android.songinfo.SongInfoQueue;

import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PlayerService extends Service implements IPlayer, SongInfoQueue.QueueObserver, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener {
    protected static final String LOG_TAG = PlayerService.class.getCanonicalName();
    protected Binder binder;

    protected MediaPlayer mediaPlayer;
    protected MediaPlayer preparingMediaPlayer;
    protected boolean startWhenPrepared = false;
    protected boolean isPlaying = false;

    protected ScheduledExecutorService changeCurrentSongService;
    protected Future<?> changeCurrentSongServiceTask;
    protected Long bufferingStartTime; // ms
    protected long bufferingTimeSum;   // ms

    protected IPlayer.IPlayerObserver playerObserver;

    protected SongInfoQueue songQueue = new SongInfoQueue();
    protected boolean hasCurrentSong = false;
    protected Calendar nextSongTimeStartedLocal;

    public synchronized void setPlayerObserver(IPlayerObserver playerObserver) {
        this.playerObserver = playerObserver;
    }

    @Override
    public synchronized void removePlayerObserver(IPlayerObserver observer) {
        this.playerObserver = null;
    }

    @Override
    public synchronized void onCreate() {
        super.onCreate();
        binder = new Binder();
        songQueue.setObserver(this);
        changeCurrentSongService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public synchronized void onDestroy() {
        super.onDestroy();
        stop();
        binder = null;
        songQueue.setObserver(null);
        changeCurrentSongService = null;
    }

    protected synchronized MediaPlayer getMediaPlayer(boolean startWhenPrepared) {
        if(mediaPlayer == null) {
            if(preparingMediaPlayer == null) {
                MediaPlayer mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(this, Uri.parse("http://listen.radiohyrule.com:8000/listen"));
                } catch(IOException e) {
                    // TODO auto-generated catch clause
                    e.printStackTrace();
                }

                preparingMediaPlayer = mediaPlayer;
                this.startWhenPrepared = this.startWhenPrepared || startWhenPrepared;
                bufferingTimeSum = 0; bufferingStartTime = null;
                mediaPlayer.setOnPreparedListener(this);
                mediaPlayer.setOnErrorListener(this);
                mediaPlayer.setOnInfoListener(this);
                mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
                mediaPlayer.prepareAsync();

                songQueue.onPlayerConnectingToStream();
            }
        } else if(startWhenPrepared) {
            mediaPlayer.start();
        }
        return mediaPlayer;
    }

    protected synchronized void startMediaPlayer() {
        getMediaPlayer(true);
    }

    protected synchronized void releaseMediaPlayer() {
        if(mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if(preparingMediaPlayer != null) {
            preparingMediaPlayer.release();
            preparingMediaPlayer = null;
        }
        songQueue.onPlayerStopRequested();
        startWhenPrepared = false;
    }

    @Override
    public synchronized void onPrepared(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
        preparingMediaPlayer = null;
        mediaPlayer.setOnPreparedListener(null);

        // update song timing information in order to estimate when the next song will start
        this.nextSongTimeStartedLocal = Calendar.getInstance();
        SongInfo currentSong = songQueue.getCurrentSong();
        if (currentSong != null) onNewPendingSong(currentSong);

        // start now
        if(startWhenPrepared) {
            startWhenPrepared = false;
            mediaPlayer.start();
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        Log.e(LOG_TAG, "media player error: " + what + ", " + extra);
        // TODO improve error handling
        stop();
        return true;
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int what, int extra) {
        Log.i(LOG_TAG, "media player info: " + what + ", " + extra);

        switch(what) {
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                bufferingStartTime = System.currentTimeMillis();
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                if(bufferingStartTime != null) {
                    bufferingTimeSum += (System.currentTimeMillis() - bufferingStartTime);
                    bufferingStartTime = null;
                    updateScheduledCurrentSongChange(getCurrentSong(), false);
                    Log.i(LOG_TAG, "media player buffering time: " + bufferingTimeSum + "ms");
                }
                break;
        }

        return false;
    }

    protected synchronized void startForegroundIntent() {
        startForegroundIntent(getCurrentSong());
    }
    protected synchronized void startForegroundIntent(SongInfo song) {
        String notificationText = "Now Playing"; //todo localize, and stringbuild
        if(song != null && song.title != null) {
            notificationText += " \"" + song.title + "\"";
        }
        Log.d(LOG_TAG, "notificationText = " + notificationText);

        Intent openListenViewIntent = new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_SELECT_NAVIGATION_ITEM_LISTEN, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, openListenViewIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_player_service_notification)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(notificationText)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(R.id.player_service, notification);
    }
    protected synchronized void stopForegroundIntent() {
        stopForeground(true);
    }

    @Override
    public synchronized boolean isPlaying() {
        return isPlaying;
    }
    public synchronized void setPlaying(boolean playing) {
        this.isPlaying = playing;
        if(this.playerObserver != null) {
            this.playerObserver.onPlaybackStateChanged(this.isPlaying);
            this.playerObserver.onCurrentSongChanged(getCurrentSong());
        }
    }

    @Override
    public synchronized void play() {
        if(!isPlaying) {
            startMediaPlayer();
            startForegroundIntent();
            setPlaying(true);
        }
    }

    @Override
    public synchronized void stop() {
        if(isPlaying) {
            releaseMediaPlayer();
            stopForegroundIntent();
            setPlaying(false);

            if(changeCurrentSongServiceTask != null) {
                changeCurrentSongServiceTask.cancel(false);
                changeCurrentSongServiceTask = null;
            }
        }
    }

    @Override
    public synchronized boolean togglePlaying() {
        if(!isPlaying) {
            play();
        } else {
            stop();
        }
        return isPlaying;
    }

    @Override
    public synchronized SongInfo getCurrentSong() {
        return isPlaying() ? songQueue.getCurrentSong() : null;
    }

    @Override
    public synchronized void onNewPendingSong(SongInfo song) {
        Log.i(LOG_TAG, "onNewPendingSong(" + (song != null ? String.valueOf(song.title) : null) + ")");

        if (song != null) {
            // update song timing information in order to estimate when the next song will start
            if (nextSongTimeStartedLocal != null) {
                song.setExpectedLocalStartTime(nextSongTimeStartedLocal);
                nextSongTimeStartedLocal = null;
                updateScheduledCurrentSongChange(song, false);
            } else {
                updateScheduledCurrentSongChange(song, true);
            }

            // this song info arrived too late, it's the current song
            if (!hasCurrentSong) {
                onCurrentSongChanged(song, false /* don't update the 'current song change' scheduled task */);
                hasCurrentSong = true;
            }
        }
    }

    protected synchronized void onCurrentSongChanged(SongInfo song, boolean updateScheduledCurrentSongChange) {
        Log.i(LOG_TAG, "onCurrentSongChanged(" + (song != null ? String.valueOf(song.title) : null) + ")");
        // update song information in service notification
        startForegroundIntent(song);

        // estimate when to update the song info again
        if(updateScheduledCurrentSongChange)
            updateScheduledCurrentSongChange(song, false);

        // inform observer
        if(playerObserver != null) playerObserver.onCurrentSongChanged(song);
    }

    protected synchronized void updateScheduledCurrentSongChange(SongInfo song, boolean onlyIfNotScheduled) {
        if (changeCurrentSongServiceTask != null) {
            if (onlyIfNotScheduled) return;
            changeCurrentSongServiceTask.cancel(false);
            changeCurrentSongServiceTask = null;
        }
        if (song != null) {
            Long timeUntilEnd = song.getEstimatedTimeUntilEnd(System.currentTimeMillis(), bufferingTimeSum);
            if (timeUntilEnd != null) {
                Log.d(LOG_TAG, "updateScheduledCurrentSongChange(); next song in " + timeUntilEnd/1000.0f + "s");
                changeCurrentSongServiceTask = changeCurrentSongService.schedule(new Runnable() {
                    @Override
                    public void run() { onChangeCurrentSong(); } }, timeUntilEnd, TimeUnit.MILLISECONDS);
            }
        }
    }

    protected synchronized void onChangeCurrentSong() {
        Log.i(LOG_TAG, "onChangeCurrentSong()");
        SongInfo song = songQueue.moveToNextSong();
        if (song != null) {
            song.setExpectedLocalStartTime(Calendar.getInstance());
            onCurrentSongChanged(song, true);
        } else {
            nextSongTimeStartedLocal = Calendar.getInstance();
            hasCurrentSong = false;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    protected class Binder extends android.os.Binder {
        public PlayerService getPlayer() {
            return PlayerService.this;
        }
    }
}
