package com.radiohyrule.android.listen;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import com.radiohyrule.android.R;
import com.radiohyrule.android.app.MainActivity;

public class PlayerService extends Service {
    protected Binder binder;
    protected MediaPlayer mediaPlayer;
    protected boolean isPlaying = false;

    @Override
    public void onCreate() {
        super.onCreate();
        binder = new Binder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopPlaying();
    }

    protected synchronized MediaPlayer getMediaPlayer() {
        if(mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, Uri.parse("http://listen.radiohyrule.com:8000/listen"));
        }
        return mediaPlayer;
    }

    protected synchronized MediaPlayer startMediaPlayer() {
        MediaPlayer result = getMediaPlayer();
        if(result != null) result.start();
        return result;
    }

    protected synchronized void stopMediaPlayer() {
        if(mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    protected synchronized void releaseMediaPlayer() {
        if(mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    protected synchronized void startForgroundIntent() {
        Intent openListenViewIntent = new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_SELECT_NAVIGATION_ITEM_LISTEN, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, openListenViewIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText("Now Playing: \"Overture\"")
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
    public synchronized void startPlaying() {
        if(!isPlaying) {
            startMediaPlayer();
            startForgroundIntent();
            isPlaying = true;
        }
    }
    public synchronized void stopPlaying() {
        if(isPlaying) {
            releaseMediaPlayer();
            stopForegroundIntent();
            isPlaying = false;
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
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class Binder extends android.os.Binder {
        public PlayerService getPlayer() {
            return PlayerService.this;
        }
    }
}
