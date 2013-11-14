package com.radiohyrule.android.listen.player;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.radiohyrule.android.R;
import com.radiohyrule.android.app.MainActivity;
import com.radiohyrule.android.listen.NowPlaying;
import com.radiohyrule.android.listen.Queue;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

public class PlayerService extends Service implements IPlayer, MediaPlayer.OnPreparedListener, Queue.QueueObserver, MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener {
    protected static final String LOG_TAG = "com.radiohyrule.android.listen.player.PlayerService";
    protected Binder binder;

    protected MediaPlayer mediaPlayer;
    protected MediaPlayer preparingMediaPlayer;
    protected boolean startWhenPrepared = false;
    protected boolean isPlaying = false;

    protected ScheduledExecutorService changeCurrentSongService;
    protected Future<?> changeCurrentSongServiceTask;
    protected Long bufferingStartTime;
    protected long bufferingTimeSum;

    protected IPlayer.IPlayerObserver playerObserver;

    protected Queue songQueue = new Queue();


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

                songQueue.onPlayerConnectingToStream(true);
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
                bufferingStartTime = System.nanoTime();
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                if(bufferingStartTime != null) {
                    bufferingTimeSum += (System.nanoTime() - bufferingStartTime);
                    bufferingStartTime = null;
                    Log.i(LOG_TAG, "media player buffering time: " + bufferingTimeSum + "ns");
                }
                break;
        }

        return false;
    }


    protected synchronized void startForgroundIntent() {
        startForgroundIntent(getCurrentSong());
    }
    protected synchronized void startForgroundIntent(NowPlaying.SongInfo song) {
        String notificationText = "Now Playing";
        if(song != null && song.getTitle() != null) {
            notificationText += " \"" + song.getTitle() + "\"";
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
            startForgroundIntent();
            setPlaying(true);
        }
    }
    @Override
    public synchronized void stop() {
        if(isPlaying) {
            releaseMediaPlayer();
            stopForegroundIntent();
            setPlaying(false);

            if(changeCurrentSongServiceTask != null) changeCurrentSongServiceTask.cancel(false);
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
    public synchronized NowPlaying.SongInfo getCurrentSong() {
        return isPlaying() ? songQueue.getCurrentSong() : null;
    }


    @Override
    public synchronized void onNewPendingSong(NowPlaying.SongInfo song) {
        Log.i(LOG_TAG, "onNewPendingSong(" + (song != null ? String.valueOf(song.getTitle()) : null) + ")");

        // XXX
        onCurrentSongChanged(song);
    }

    @Override
    public synchronized void onCurrentSongChanged(NowPlaying.SongInfo song) {
        Log.i(LOG_TAG, "onCurrentSongChanged(" + (song != null ? String.valueOf(song.getTitle()) : null) + ")");
        // update song information in service notification
        startForgroundIntent(song);

        // inform observer
        if(playerObserver != null) playerObserver.onCurrentSongChanged(song);
    }


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class Binder extends android.os.Binder {
        public PlayerService getPlayer() {
            return PlayerService.this;
        }
    }
}
