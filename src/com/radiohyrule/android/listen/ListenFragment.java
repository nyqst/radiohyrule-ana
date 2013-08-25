package com.radiohyrule.android.listen;

import android.widget.ImageButton;
import com.radiohyrule.android.app.BaseFragment;
import com.radiohyrule.android.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.radiohyrule.android.app.MainActivity;

public class ListenFragment extends BaseFragment implements IPlayer.IPlayerObserver {
    protected ImageButton buttonPlayStop;
    protected ImageButton buttonFavouriteCurrent;
    protected static final String saveKey_buttonFavouriteCurrent_isSelected = "buttonFavouriteCurrent_isSelected";

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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(saveKey_buttonFavouriteCurrent_isSelected, buttonFavouriteCurrent.isSelected());
    }


    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        if(this.buttonPlayStop != null) {
            this.buttonPlayStop.setSelected(isPlaying);
        }
    }
}
