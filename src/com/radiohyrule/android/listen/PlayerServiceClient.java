package com.radiohyrule.android.listen;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public class PlayerServiceClient implements IPlayer {
    protected final Context context;

    protected Intent playerServiceIntent;
    protected PlayerService playerService;
    protected boolean playOnConnection = false;
    protected boolean isStarting = false;

    public PlayerServiceClient(Context context) {
        this.context = context;
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
    protected synchronized void onConnected(PlayerService playerService) {
        isStarting = false;

        this.playerService = playerService;
        if(playerService != null) {
            if(playOnConnection) {
                playOnConnection = false;
                playerService.startPlaying();
            }
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


    protected synchronized void startService() {
        if(playerService == null && !isStarting) {
            playerServiceIntent = new Intent(context, PlayerService.class);
            context.bindService(playerServiceIntent, playerServiceConnection, Context.BIND_AUTO_CREATE);
            context.startService(playerServiceIntent);
            isStarting = true;
        }
    }

    protected synchronized void stopService() {
        if(playerService != null || isStarting) {
            context.unbindService(playerServiceConnection);
            playerService = null;
            context.stopService(playerServiceIntent);
            playerServiceIntent = null;
        }
    }

    private ServiceConnection playerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            PlayerService.Binder binder = (PlayerService.Binder) service;
            PlayerService playerService = binder.getPlayer();
            PlayerServiceClient.this.onConnected(playerService);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            PlayerServiceClient.this.stopService();
        }
    };
}
