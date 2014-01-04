package com.radiohyrule.android.listen;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.felipecsl.android.imaging.ImageManager;
import com.radiohyrule.android.R;
import com.radiohyrule.android.app.BaseFragment;
import com.radiohyrule.android.app.MainActivity;
import com.radiohyrule.android.listen.player.IPlayer;
import com.radiohyrule.android.opengl.BlurredSurfaceRenderer;
import com.radiohyrule.android.opengl.Util;
import com.radiohyrule.android.util.GraphicsUtil;

public class ListenFragment extends BaseFragment implements IPlayer.IPlayerObserver {
    protected ImageView imageCover, imageBackground;
    protected GLSurfaceView surfaceBackground;
    protected BlurredSurfaceRenderer surfaceRenderer;
    protected TextView textArtist, textArtistTitleSeparator, textTitle, textAlbum;
    protected View layoutArtistTitleLine;
    protected TextView textRequestedBy, textNumListeners;
    protected TextView textTimeElapsed, textTimeRemaining;
    protected ProgressBar progressTime;
    protected ImageButton buttonShowInLibrary, buttonPlayStop, buttonFavourite;
    protected View titleView;

    protected static final String saveKey_buttonFavourite_isSelected = "buttonFavourite_isSelected";


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
        if(rootView == null) return null;

        imageCover = (ImageView) rootView.findViewById(R.id.listen_image_cover);
        imageBackground = (ImageView) rootView.findViewById(R.id.listen_image_background);
        if (Util.getOpenGlVersion(getActivity()) >= Util.OPENGL2) {
            surfaceBackground = new GLSurfaceView(getActivity());
            surfaceBackground.setEGLContextClientVersion(2);
            surfaceRenderer = new BlurredSurfaceRenderer();
            surfaceBackground.setRenderer(surfaceRenderer);
            surfaceBackground.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
            ViewGroup backgroundParent = (ViewGroup)imageBackground.getParent();
            int index = backgroundParent.indexOfChild(imageBackground);
            backgroundParent.removeViewAt(index);
            surfaceBackground.setLayoutParams(imageBackground.getLayoutParams());
            backgroundParent.addView(surfaceBackground, index);
            imageBackground = null;
        }

        textArtist = (TextView) rootView.findViewById(R.id.listen_text_artist);
        textTitle = (TextView) rootView.findViewById(R.id.listen_text_title);
        textAlbum = (TextView) rootView.findViewById(R.id.listen_text_album);
        if(textArtist == null || textTitle == null || textAlbum == null) {
            titleView = inflater.inflate(R.layout.fragment_listen_title, null);
            textArtist = (TextView) titleView.findViewById(R.id.listen_text_artist);
            textArtistTitleSeparator = (TextView) titleView.findViewById(R.id.listen_text_artist_title_separator);
            textTitle = (TextView) titleView.findViewById(R.id.listen_text_title);
            textAlbum = (TextView) titleView.findViewById(R.id.listen_text_album);
            layoutArtistTitleLine = titleView.findViewById(R.id.listen_layout_artist_title_line);
        } else {
            titleView = null;
            textArtistTitleSeparator = null;
            layoutArtistTitleLine = null;
        }

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
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        onTitleChanged();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(saveKey_buttonFavourite_isSelected, buttonFavourite.isSelected());
    }


    protected synchronized void showCurrentSongInfo(NowPlaying.SongInfo song) {
        if(song != null) {
            String albumCover = song.getAlbumCover();
            if (albumCover != null) {
                ImageManager imageManager = getMainActivity().getImageManager();
                if (imageManager != null) {
                    Uri imageUri = Uri.withAppendedPath(Uri.parse("http://radiohyrule.com/cover640/"), song.getAlbumCover());
                    imageManager.setBitmapCallback(new ImageManager.BitmapCallback() {
                        @Override
                        public void onBitmapLoaded(Bitmap bitmap) {
                            Context context = getActivity();
                            bitmap = GraphicsUtil.getRoundedCorners(bitmap, GraphicsUtil.dpToPx(context, 5));
                            imageCover.setImageBitmap(bitmap);
                            if (imageBackground != null) {
                                //imageBackground.setImageBitmap(GraphicsUtil.blurImage(bitmap, context));
                            } else if (surfaceRenderer != null) {
                                surfaceRenderer.setNextBitmap(bitmap);
                                surfaceBackground.requestRender();
                            }
                        }
                    });
                    imageManager.loadImage(imageUri.toString());
                }
            }

            String defaultTitleText = titleView == null ? "--" : "";
            if(textTitle != null) {
                textArtist.setText(defaultTitleText);
                Iterable<String> artists = song.getArtists();
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
            if(textTitle != null) textTitle.setText(song.getTitle() != null ? song.getTitle() : defaultTitleText);
            if(textAlbum != null) textAlbum.setText(song.getAlbum() != null ? song.getAlbum() : defaultTitleText);
            if(titleView != null) {
                if(textArtist != null) textArtist.setVisibility(textArtist.getText().length() == 0 ? View.GONE : View.VISIBLE);
                if(textTitle != null) textTitle.setVisibility(textTitle.getText().length() == 0 ? View.GONE : View.VISIBLE);
                if(textAlbum != null) textAlbum.setVisibility(textAlbum.getText().length() == 0 ? View.GONE : View.VISIBLE);
                if(textArtistTitleSeparator != null) {
                    if(textArtist == null || textArtist.getVisibility() == View.GONE || textTitle == null || textTitle.getVisibility() == View.GONE) {
                        textArtistTitleSeparator.setVisibility(View.GONE);
                    } else {
                        textArtistTitleSeparator.setVisibility(View.VISIBLE);
                    }
                }
                if(layoutArtistTitleLine != null) {
                    if((textArtist == null || textArtist.getVisibility() == View.GONE) && (textTitle == null || textTitle.getVisibility() == View.GONE)) {
                        layoutArtistTitleLine.setVisibility(View.GONE);
                    } else {
                        layoutArtistTitleLine.setVisibility(View.VISIBLE);
                    }
                }
            }

            if(textRequestedBy != null) {
                if(song.getRequestUsername() != null) {
                    textRequestedBy.setText("Requested by " + song.getRequestUsername());
                    textRequestedBy.setVisibility(View.VISIBLE);
                } else {
                    textRequestedBy.setText(null);
                    textRequestedBy.setVisibility(View.GONE);
                }
            }
            if(textNumListeners != null) textNumListeners.setText(String.valueOf(song.getNumListenersValue()));

            // TODO textTimeElapsed
            if(textTimeRemaining != null) {
                int secondsRemaining = (int) Math.floor(song.getDurationValue() + 0.5); // TODO
                String timeRemaining = String.format("%d:%02d", secondsRemaining/60, secondsRemaining%60);
                textTimeRemaining.setText(timeRemaining);
            }
            // TODO progressTime

        } else {
            // TODO imageCover, imageBackground
            imageCover.setImageResource(R.drawable.cover_blank);
            if (imageBackground != null) {
                imageBackground.setImageResource(R.drawable.cover_blank_background);
            } else if (surfaceRenderer != null) {
                surfaceRenderer.setNextBitmap(null);
                surfaceBackground.requestRender();
            }

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
        if(buttonPlayStop != null) {
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
    public void onCurrentSongChanged(final NowPlaying.SongInfo song) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() { showCurrentSongInfo(song); }
        });
        if(titleView != null) {
            onTitleChanged();
        }
    }
}
