package com.radiohyrule.android.fragments;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.radiohyrule.android.R;
import com.radiohyrule.android.api.types.SongInfo;
import com.radiohyrule.android.player.ExoService;
import com.squareup.picasso.Picasso;

import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;

public class NewListenFragment extends Fragment implements ServiceConnection{

    public static final String LOG_TAG = NewListenFragment.class.getSimpleName();
    public static final String ALBUM_ART_URL_BASE = "https://radiohyrule.com/cover640/";

    @Bind(R.id.listen_button_play_stop) ImageButton playButton;

    @Bind(R.id.listen_text_title) TextView textSongTitle;
    @Bind(R.id.listen_text_artist) TextView textSongArtist;
    @Bind(R.id.listen_text_time_elapsed) TextView textTimeElapsed;
    @Bind(R.id.listen_text_time_remaining) TextView textTimeLeft; //todo handle these
    @Bind(R.id.listen_text_requested_by) TextView textRequester;
    @Bind(R.id.listen_text_num_listeners) TextView textListenerCount;

    @Bind(R.id.listen_progress_time) ProgressBar seekBar;
    @Bind(R.id.listen_image_cover) ImageView albumArtView;

    @Nullable
    private ExoService exoService;
    private Application appContext;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_listen, container, false);
        ButterKnife.bind(this, rootView);

        playButton.setOnClickListener(playButtonClickListener);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.v(LOG_TAG, "Started");
        appContext =(Application)getActivity().getApplicationContext();
        appContext.bindService(new Intent(getActivity(), ExoService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.v(LOG_TAG, "Stopped");
        if (exoService != null) {
            exoService.unregisterListener(playbackStatusListener);
        }
        appContext.unbindService(this);
        exoService = null;
    }

    private View.OnClickListener playButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (exoService != null) {
                ExoService.PlaybackStatus playbackStatus = exoService.getCurrentPlaybackStatus();
                switch (playbackStatus) {
                    case PLAYING:
                        exoService.stopPlayback();
                        break;
                    case PAUSED:
                        exoService.startPlayback();
                        break;
                    case BUFFERING:
                    default:
                        exoService.stopPlayback();
                        break;
                }

            }
        }
    };

    private ExoService.PlaybackStatusListener playbackStatusListener = new ExoService.PlaybackStatusListener() {
        @Override
        public void onPlaybackStatusChanged(ExoService.PlaybackStatus status) {
            switch (status) {
                case PLAYING:
                    playButton.setImageResource(R.drawable.ic_av_pause_blue);
                    if(exoService != null && exoService.getCachedSongInfo() != null){
                        onNewMetadataAvailable(exoService.getCachedSongInfo());
                    }
                    break;
                case PAUSED:
                    playButton.setImageResource(R.drawable.ic_av_play_white);
                    //todo clear info
                    break;
                case BUFFERING:
                default:
                    playButton.setImageResource(R.drawable.ic_av_pause_blue);
//                    playButton.setImageResource(R.drawable.ic_av_play_blue);
                    //todo figure out how to debounce status
                    //to prevent blue play button flicker on resuming stream
                    break;
            }
        }

        @Override
        public void onError(Throwable throwable) {
            Log.e(LOG_TAG, "Error during playback: ", throwable);
        }

        @Override
        public void onNewMetadataAvailable(@NonNull SongInfo songInfo) {
            //the listener is bracketed by start/stop calls, so it should always be safe to modify UI from here.
            Log.v(LOG_TAG, "Got new metadata title: " + songInfo.title);
            textSongTitle.setText(songInfo.title);
            textSongArtist.setText(TextUtils.join(", ", songInfo.artists));
            textListenerCount.setVisibility(View.VISIBLE);
            textListenerCount.setText(String.format(Locale.getDefault(), "%d", songInfo.numListeners));
            textRequester.setText(songInfo.requestUsername);
            if(songInfo.albumCover != null && !songInfo.albumCover.isEmpty()) {
                Uri imageUri = Uri.withAppendedPath(Uri.parse(ALBUM_ART_URL_BASE), songInfo.albumCover);
                Picasso.with(getActivity())
                        .load(imageUri)
                        .placeholder(R.drawable.cover_blank)
                        .fit().centerInside()
                        .into(albumArtView);
            }
        }
    };

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        playButton.setEnabled(true);
        exoService = ((ExoService.PlaybackBinder) service).getExoService();
        exoService.registerNewPlaybackStatusListener(playbackStatusListener);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        exoService = null;
        playButton.setImageResource(R.drawable.ic_av_play_grey);
        playButton.setEnabled(false);
    }
}
