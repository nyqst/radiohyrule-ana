package com.radiohyrule.android.fragments;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.radiohyrule.android.R;
import com.radiohyrule.android.activities.MainActivity;
import com.radiohyrule.android.player.IPlayer;
import com.radiohyrule.android.songinfo.SongInfo;
import com.squareup.picasso.Picasso;

import java.util.Locale;

public class ListenFragment extends BaseFragment implements IPlayer.IPlayerObserver {
    public static final String ALBUM_ART_URL_BASE = "https://radiohyrule.com/cover640/";
    protected ImageView imageCover;
    protected TextView textArtist, textArtistTitleSeparator, textTitle, textAlbum;
    protected View layoutArtistTitleLine;
    protected TextView textRequestedBy, textNumListeners;
    protected TextView textTimeElapsed, textTimeRemaining;
    protected ProgressBar progressTime;
    protected ImageButton buttonPlayStop;
    protected View titleView;

    @Override
    public String getTitleText() {
        return "Listen";
    }
    @Override
    public Object getTitle() {
        IPlayer player = getPlayer();
        if(player != null && player.getCurrentSong() != null && titleView != null) {
            return titleView;
        } else {
            return super.getTitle();
        }
    }
    protected void onTitleChanged() {
        MainActivity mainActivity = getMainActivity();
        if(mainActivity != null) {
            mainActivity.onTitleChanged(this.getTitle());
        }
    }

    protected IPlayer getPlayer() {
        MainActivity mainActivity = getMainActivity();
        if(mainActivity != null) {
            return mainActivity.getPlayer();
        } else {
            return null;
        }
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

        imageCover = (ImageView) rootView.findViewById(R.id.listen_image_cover);

        textArtist = (TextView) rootView.findViewById(R.id.listen_text_artist);
        textTitle = (TextView) rootView.findViewById(R.id.listen_text_title);
        textAlbum = (TextView) rootView.findViewById(R.id.listen_text_album);
        if(textArtist == null || textTitle == null || textAlbum == null) { //todo what is going on here
            titleView = inflater.inflate(R.layout.fragment_listen_title, null);
            textArtist = (TextView) titleView.findViewById(R.id.listen_text_artist);
            textTitle = (TextView) titleView.findViewById(R.id.listen_text_title);
            textAlbum = (TextView) titleView.findViewById(R.id.listen_text_album);
        } else {
            titleView = null;
        }

        textRequestedBy = (TextView) rootView.findViewById(R.id.listen_text_requested_by);
        textNumListeners = (TextView) rootView.findViewById(R.id.listen_text_num_listeners);

        textTimeElapsed = (TextView) rootView.findViewById(R.id.listen_text_time_elapsed);
        textTimeRemaining = (TextView) rootView.findViewById(R.id.listen_text_time_remaining);
        progressTime = (ProgressBar) rootView.findViewById(R.id.listen_progress_time);

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

        showCurrentSongInfo(getPlayer().getCurrentSong());

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        onTitleChanged();
    }

    protected synchronized void showCurrentSongInfo(SongInfo song) {
        if(song != null) {
            String albumCover = song.albumCover;
            if (albumCover != null) {
                Uri imageUri = Uri.withAppendedPath(Uri.parse(ALBUM_ART_URL_BASE), song.albumCover);
                Picasso.with(getActivity())
                        .load(imageUri)
                        .placeholder(R.raw.cover_default)
                        .error(R.raw.cover_default)
                        .into(imageCover);
                Log.v(ListenFragment.class.getSimpleName(), "Loaded album art: " + albumCover);
            }

            String defaultTitleText = titleView == null ? "--" : "";
            if(textTitle != null) {
                textArtist.setText(defaultTitleText);
                Iterable<String> artists = song.artists;
                if(artists != null) {
                    StringBuilder sb = new StringBuilder();
                    boolean first = true;
                    for(String artist : artists) {
                        if(artist != null) {
                            if(!first) sb.append(", ");
                            sb.append(artist);
                            first = false;
                        }
                    }
                    if(sb.length() > 0) textArtist.setText(sb.toString());
                }
            }
            if(textTitle != null) textTitle.setText(!song.title.isEmpty() ? song.title : defaultTitleText);
            if(textAlbum != null) textAlbum.setText(song.album != null && !song.album.isEmpty() ? song.album : defaultTitleText);
            if(titleView != null) {
                if(textArtist != null) textArtist.setVisibility(textArtist.getText().length() == 0 ? View.GONE : View.VISIBLE);
                if(textTitle != null) textTitle.setVisibility(textTitle.getText().length() == 0 ? View.GONE : View.VISIBLE);
                if(textAlbum != null) textAlbum.setVisibility(textAlbum.getText().length() == 0 ? View.GONE : View.VISIBLE);
            }

            if(textRequestedBy != null) {
                if(song.requestUsername != null) {
                    textRequestedBy.setText("Requested by " + song.requestUsername);
                    textRequestedBy.setVisibility(View.VISIBLE);
                } else {
                    textRequestedBy.setText(null);
                    textRequestedBy.setVisibility(View.GONE);
                }
            }
            if(textNumListeners != null) textNumListeners.setText(String.valueOf(song.numListeners));

            // TODO textTimeElapsed
            if(textTimeRemaining != null) {
                int secondsRemaining = (int) Math.floor(song.duration + 0.5); // TODO
                String timeRemaining = String.format(Locale.ENGLISH, "%d:%02d", secondsRemaining/60, secondsRemaining%60);
                textTimeRemaining.setText(timeRemaining);
            }
            // TODO progressTime

        } else {
            // TODO imageCover, imageBackground
            Picasso.with(getActivity()).load(R.raw.cover_blank).into(imageCover);

            if(titleView != null) {
                if(textArtist != null) textArtist.setText(null);
                if(textTitle != null) textTitle.setText(null);
                if(textAlbum != null) textAlbum.setText(null);
            } else {
                if(textArtist != null) textArtist.setText("--");
                if(textTitle != null) textTitle.setText("--");
                if(textAlbum != null) textAlbum.setText("--");
            }

            if(textRequestedBy  != null) {
                textRequestedBy.setText(null);
                textRequestedBy.setVisibility(View.GONE);
            }
            if(textNumListeners != null) textNumListeners.setText("--");

            if(textTimeElapsed   != null) textTimeElapsed.setText("-:--");
            if(textTimeRemaining != null) textTimeRemaining.setText("-:--");
            if(progressTime      != null) progressTime.setProgress(0);
        }
    }


    @Override
    public void onPlaybackStateChanged(final boolean isPlaying) {
        final ImageButton buttonPlayStop = this.buttonPlayStop;
        if(buttonPlayStop != null && getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    buttonPlayStop.setSelected(isPlaying);
                    IPlayer player = getPlayer();
                    if(player != null) {
                        showCurrentSongInfo(player.getCurrentSong());
                    }
                }
            });
        }
        if(titleView != null) {
            onTitleChanged();
        }
    }

    @Override
    public void onCurrentSongChanged(final SongInfo song) {
        if(getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showCurrentSongInfo(song);
                }
            });
        }
        if(titleView != null) {
            onTitleChanged();
        }
    }
}
