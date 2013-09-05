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

public class PlayerService extends Service implements MediaPlayer.OnPreparedListener, Queue.QueueObserver {
    protected static final String LOG_TAG = "com.radiohyrule.android.listen.player.PlayerService";
    protected Binder binder;

    protected MediaPlayer mediaPlayer;
    protected MediaPlayer preparingMediaPlayer;
    protected boolean startWhenPrepared = false;
    protected boolean isPlaying = false;

    protected IPlayer.IPlayerObserver playerObserver;

    protected Queue songQueue = new Queue();


    public void setPlayerObserver(IPlayer.IPlayerObserver playerObserver) {
        this.playerObserver = playerObserver;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        binder = new Binder();
        songQueue.setObserver(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopPlaying();
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
                mediaPlayer.setOnPreparedListener(this);
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

    protected synchronized void startForgroundIntent() {
        String notificationText = "Now Playing";
        NowPlaying.SongInfo song = songQueue.getCurrentSong();
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

    public synchronized boolean isPlaying() {
        return isPlaying;
    }
    public synchronized void setPlaying(boolean playing) {
        this.isPlaying = playing;
        if(this.playerObserver != null)
            this.playerObserver.onPlaybackStateChanged(this.isPlaying);
    }
    public synchronized void startPlaying() {
        if(!isPlaying) {
            startMediaPlayer();
            startForgroundIntent();
            setPlaying(true);
        }
    }
    public synchronized void stopPlaying() {
        if(isPlaying) {
            releaseMediaPlayer();
            stopForegroundIntent();
            setPlaying(false);
        }
    }
    public synchronized boolean togglePlaying() {
        if(!isPlaying) {
            startPlaying();
        } else {
            stopPlaying();
        }
        return isPlaying;
    }


    @Override
    public void onNewPendingSong(NowPlaying.SongInfo song) {
        Log.i(LOG_TAG, "onNewPendingSong(" + (song != null ? String.valueOf(song.getTitle()) : null) + ")");
    }

    @Override
    public void onCurrentSongChanged(NowPlaying.SongInfo song) {
        Log.i(LOG_TAG, "onCurrentSongChanged(" + (song != null ? String.valueOf(song.getTitle()) : null) + ")");
        // update song information in service notification
        startForgroundIntent();
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
