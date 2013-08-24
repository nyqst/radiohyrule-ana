package com.radiohyrule.android.listen;

import android.media.MediaPlayer;
import android.net.Uri;
import android.widget.ImageButton;
import com.radiohyrule.android.app.BaseFragment;
import com.radiohyrule.android.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ListenFragment extends BaseFragment {
    protected ImageButton buttonPlayStop;
    protected static final String saveKey_buttonPlayStop_isSelected = "buttonPlayStop_isSelected";
    protected ImageButton buttonFavouriteCurrent;
    protected static final String saveKey_buttonFavouriteCurrent_isSelected = "buttonFavouriteCurrent_isSelected";

    protected MediaPlayer mediaPlayer;

    @Override
    public String getTitle() {
        return "Listen";
    }

    protected synchronized MediaPlayer getMediaPlayer() {
        if(mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(getActivity(), Uri.parse("http://listen.radiohyrule.com:8000/listen"));
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_listen, container, false);
        if(rootView == null) return null;

        buttonPlayStop = (ImageButton) rootView.findViewById(R.id.listen_button_play_stop);
        if(buttonPlayStop != null) {
            buttonPlayStop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean isPlaying = view.isSelected();
                    if(isPlaying) releaseMediaPlayer();
                    else startMediaPlayer();
                    view.setSelected(!isPlaying);
                }
            });
            if(savedInstanceState != null)
                buttonPlayStop.setSelected(savedInstanceState.getBoolean(saveKey_buttonPlayStop_isSelected));
        }

        buttonFavouriteCurrent = (ImageButton) rootView.findViewById(R.id.listen_button_favourite_current);
        if(buttonFavouriteCurrent != null) {
            buttonFavouriteCurrent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    view.setSelected(!view.isSelected());
                }
            });
            if(savedInstanceState != null)
                buttonFavouriteCurrent.setSelected(savedInstanceState.getBoolean(saveKey_buttonFavouriteCurrent_isSelected));
        }

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(saveKey_buttonPlayStop_isSelected, buttonPlayStop.isSelected());
        outState.putBoolean(saveKey_buttonFavouriteCurrent_isSelected, buttonFavouriteCurrent.isSelected());
    }
}
