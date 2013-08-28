package com.radiohyrule.android.listen;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class PlayerServiceClient implements IPlayer, IPlayer.IPlayerObserver {
    private static final String LOG_TAG = "com.radiohyrule.android.listen.PlayerServiceClient";

    protected Context context;

    protected IPlayerObserver observer;

    protected PlayerService playerService;
    protected boolean playOnServiceBound = false;


    public PlayerServiceClient(Context context) {
        setContext(context);
    }
    public synchronized void setContext(Context context) {
        if(context != this.context) {
            Log.d(LOG_TAG, "setContext(" + String.valueOf(context) + ")");
            if(context == null) {
                // no new context
                // stop the service if the playback is stopped
                if(playerService != null && !isPlaying()) {
                    stopService();
                } else {
                    // player is currently active or there is no current connection to the service
                    // (so we can't be sure whether it's playing or not).
                    // keep the service running, unbind only
                    unbindService();
                }
                // remove old context
                this.context = null;

            } else {
                // setting a new context
                // unbind old context, but keep the service running
                unbindService();
                // use new context
                this.context = context;
                // start service and bind the new context to it
                startAndBindService();
            }
        }
    }


    @Override
    public void setPlayerObserver(IPlayerObserver observer) {
        this.observer = observer;
    }
    @Override
    public void removePlayerObserver(IPlayerObserver observer) {
        if(this.observer == observer)
            this.observer = null;
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        Log.v(LOG_TAG, "onPlaybackStateChanged(" + String.valueOf(isPlaying) + ")");
        if(this.observer != null)
            this.observer.onPlaybackStateChanged(isPlaying);
    }


    @Override
    public synchronized boolean isPlaying() {
        if(playerService == null) {
            return false;
        } else {
            return playerService.isPlaying();
        }
    }

    @Override
    public synchronized void play() {
        if(playerService == null) {
            playOnServiceBound = true;
            startAndBindService();
        } else {
            playerService.startPlaying();
        }
    }

    @Override
    public synchronized void stop() {
        if(playerService != null) {
            playerService.stopPlaying();
        }
        playOnServiceBound = false;
    }

    @Override
    public synchronized void togglePlaying() {
        if(playerService == null) {
            play();
        } else {
            boolean isPlaying = playerService.togglePlaying();
            if(!isPlaying) {
                stopService();
            }
        }
    }


    protected synchronized void startAndBindService() {
        if(context != null) {
            Log.d(LOG_TAG, "startAndBindService()");
            Intent playerServiceIntent = new Intent(context, PlayerService.class);
            context.startService(playerServiceIntent);

            playerServiceConnection.bindService(context, playerService, playerServiceIntent);
        }
    }
    protected synchronized void onServiceBound(PlayerService playerService) {
        this.playerService = playerService;
        if(playerService != null) {
            Log.d(LOG_TAG, "onServiceBound(); isPlaying ==" + String.valueOf(this.playerService.isPlaying()));
            onPlaybackStateChanged(this.playerService.isPlaying());
            this.playerService.setPlayerObserver(this);

            if(playOnServiceBound) {
                playOnServiceBound = false;
                playerService.startPlaying();
            }
        }
    }

    protected synchronized void stopService() {
        if(context != null) {
            Log.d(LOG_TAG, "stopService()");
            unbindService();

            Intent playerServiceIntent = new Intent(context, PlayerService.class);
            context.stopService(playerServiceIntent);
        }
    }
    protected synchronized void unbindService() {
        if(context != null) {
            Log.d(LOG_TAG, "unbindService()");
            playerServiceConnection.unbindService(context, playerService);
            playerService = null;
        }
    }
    protected synchronized void onServiceUnbound() {
        // connection lost // TODO restart?
        Log.d(LOG_TAG, "onServiceUnbound()");
        playerService = null;
    }

    protected class ServiceConnection implements android.content.ServiceConnection {
        protected boolean isPending = false;

        public synchronized void bindService(Context context, PlayerService playerService, Intent playerServiceIntent) {
            if(playerService == null && !isPending) {
                context.bindService(playerServiceIntent, this, Context.BIND_AUTO_CREATE);
                isPending = true;
            }
        }
        public synchronized void unbindService(Context context, PlayerService playerService) {
            if(playerService != null || isPending) {
                context.unbindService(this);
                isPending = false;
            }
        }

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            PlayerService.Binder binder = (PlayerService.Binder) service;
            PlayerService playerService = binder.getPlayer();

            isPending = false;
            PlayerServiceClient.this.onServiceBound(playerService);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            PlayerServiceClient.this.onServiceUnbound();
            isPending = true;
        }
    }
    protected ServiceConnection playerServiceConnection = new ServiceConnection();
}
