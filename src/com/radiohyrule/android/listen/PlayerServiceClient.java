package com.radiohyrule.android.listen;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public class PlayerServiceClient implements IPlayer, IPlayer.IPlayerObserver {
    protected Context context;

    protected IPlayerObserver observer;

    protected Intent playerServiceIntent;
    protected PlayerService playerService;
    protected boolean playOnConnection = false;
    protected boolean isStarting = false;


    public PlayerServiceClient(Context context) {
        this.context = context;
    }
    public synchronized void setContext(Context context) {
        if(context != this.context) {
            // unbind old context
            if(this.context != null && playerServiceConnectionBound) {
                this.context.unbindService(playerServiceConnection);
                playerServiceConnectionBound = false;
            }

            // bind new context
            this.context = context;
            if(context != null && playerServiceIntent != null) {
                Intent intent = new Intent(context, PlayerService.class);
                this.context.bindService(intent, playerServiceConnection, Context.BIND_AUTO_CREATE);
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
            playOnConnection = true;
            startService();
        } else {
            playerService.startPlaying();
        }
    }

    @Override
    public synchronized void stop() {
        if(playerService != null) {
            playerService.stopPlaying();
            stopService();
        }
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


    protected synchronized void onConnected(PlayerService playerService) {
        isStarting = false;
        playerServiceConnectionBound = true;

        this.playerService = playerService;
        if(playerService != null) {
            onPlaybackStateChanged(this.playerService.isPlaying());
            this.playerService.setPlayerObserver(this);

            if(playOnConnection) {
                playOnConnection = false;
                playerService.startPlaying();
            }
        }
    }

    protected synchronized void onDisconnected() {
        stopService();

        playerServiceConnectionBound = false;
    }


    protected synchronized void startService() {
        if(playerService == null && !isStarting) {
            playerServiceIntent = new Intent(context, PlayerService.class);
            context.startService(playerServiceIntent);
            context.bindService(playerServiceIntent, playerServiceConnection, Context.BIND_AUTO_CREATE);
            isStarting = true;
        }
    }

    protected synchronized void stopService() {
        if(playerService != null || isStarting) {
            context.unbindService(playerServiceConnection);
            playerServiceConnectionBound = false;
            playerService = null;

            context.stopService(playerServiceIntent);
            playerServiceIntent = null;
        }
    }

    protected boolean playerServiceConnectionBound = false;
    protected ServiceConnection playerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            PlayerService.Binder binder = (PlayerService.Binder) service;
            PlayerService playerService = binder.getPlayer();
            PlayerServiceClient.this.onConnected(playerService);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            PlayerServiceClient.this.onDisconnected();
        }
    };
}
