package com.radiohyrule.android.listen;

import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.radiohyrule.android.app.BaseFragment;
import com.radiohyrule.android.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.radiohyrule.android.app.MainActivity;
import com.radiohyrule.android.listen.player.IPlayer;

public class ListenFragment extends BaseFragment implements IPlayer.IPlayerObserver {
    protected ImageView imageCover, imageBackground;
    protected TextView textArtist, textTitle, textAlbum;
    protected TextView textRequestedBy, textNumListeners;
    protected TextView textTimeElapsed, textTimeRemaining;
    protected ProgressBar progressTime;
    protected ImageButton buttonShowInLibrary, buttonPlayStop, buttonFavourite;

    protected static final String saveKey_buttonFavourite_isSelected = "buttonFavourite_isSelected";


    @Override
    public String getTitle() {
        return "Listen";
    }


    protected IPlayer getPlayer() {
        MainActivity mainActivity = (MainActivity) getActivity();
        return mainActivity.getPlayer();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPlayer().setPlayerObserver(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        getPlayer().removePlayerObserver(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_listen, container, false);
        if(rootView == null) return null;

        imageCover = (ImageView) rootView.findViewById(R.id.listen_image_cover);
        imageBackground = (ImageView) rootView.findViewById(R.id.listen_image_background);

        textArtist = (TextView) rootView.findViewById(R.id.listen_text_artist);
        textTitle = (TextView) rootView.findViewById(R.id.listen_text_title);
        textAlbum = (TextView) rootView.findViewById(R.id.listen_text_album);

        textRequestedBy = (TextView) rootView.findViewById(R.id.listen_text_requested_by);
        textNumListeners = (TextView) rootView.findViewById(R.id.listen_text_num_listeners);

        textTimeElapsed = (TextView) rootView.findViewById(R.id.listen_text_time_elapsed);
        textTimeRemaining = (TextView) rootView.findViewById(R.id.listen_text_time_remaining);
        progressTime = (ProgressBar) rootView.findViewById(R.id.listen_progress_time);

        buttonShowInLibrary = (ImageButton) rootView.findViewById(R.id.listen_button_show_in_library);

        buttonPlayStop = (ImageButton) rootView.findViewById(R.id.listen_button_play_stop);
        if(buttonPlayStop != null) {
            buttonPlayStop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getPlayer().togglePlaying();
                }
            });
        }
        onPlaybackStateChanged(getPlayer().isPlaying());

        buttonFavourite = (ImageButton) rootView.findViewById(R.id.listen_button_favourite);
        if(buttonFavourite != null) {
            buttonFavourite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) { view.setSelected(!view.isSelected()); }
            });
            if(savedInstanceState != null)
                buttonFavourite.setSelected(savedInstanceState.getBoolean(saveKey_buttonFavourite_isSelected));
        }

        showCurrentSongInfo(getPlayer().getCurrentSong());

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(saveKey_buttonFavourite_isSelected, buttonFavourite.isSelected());
    }


    protected synchronized void showCurrentSongInfo(NowPlaying.SongInfo song) {
        if(song != null) {
            // TODO imageCover, imageBackground

            if(textArtist != null) {
                Iterable<String> artists = song.getArtists();
                if(artists != null) {
                    StringBuilder sb = new StringBuilder();
                    boolean first = true;
                    for(String artist : song.getArtists()) {
                        if(artist != null) {
                            if(!first) sb.append(", ");
                            sb.append(artist);
                            first = false;
                        }
                    }
                    textArtist.setText(sb.toString());
                } else {
                    textArtist.setText("--");
                }
            }
            if(textTitle != null) textTitle.setText(song.getTitle() != null ? song.getTitle() : "--");
            if(textAlbum != null) textAlbum.setText(song.getAlbum() != null ? song.getAlbum() : "--");

            if(textRequestedBy != null) {
                if(song.getRequestUsername() != null) {
                    textRequestedBy.setText("Requested by " + song.getRequestUsername());
                } else {
                    textRequestedBy.setText(null);
                }
            }
            if(textNumListeners != null) textNumListeners.setText(String.valueOf(song.getNumListenersValue()));

            // TODO textTimeElapsed
            if(textTimeRemaining != null) {
                int secondsRemaining = (int) Math.floor(song.getDurationValue() + 0.5); // TODO
                String timeRemaining = "-"+String.valueOf(secondsRemaining/60)+":"+String.valueOf(secondsRemaining%60);
                textTimeRemaining.setText(timeRemaining);
            }
            // TODO progressTime

        } else {
            // TODO imageCover, imageBackground

            if(textArtist != null) textArtist.setText("--");
            if(textTitle  != null) textTitle.setText("--");
            if(textAlbum  != null) textAlbum.setText("--");

            if(textRequestedBy  != null) textRequestedBy.setText(null);
            if(textNumListeners != null) textNumListeners.setText("--");

            if(textTimeElapsed   != null) textTimeElapsed.setText("-:--");
            if(textTimeRemaining != null) textTimeRemaining.setText("-:--");
            if(progressTime      != null) progressTime.setProgress(0);
        }
    }


    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        if(this.buttonPlayStop != null) {
            this.buttonPlayStop.setSelected(isPlaying);
        }
    }

    @Override
    public void onCurrentSongChanged(final NowPlaying.SongInfo song) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() { showCurrentSongInfo(song); }
        });
    }
}
