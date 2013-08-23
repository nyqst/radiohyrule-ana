package com.radiohyrule.android.listen;

import com.radiohyrule.android.app.BaseFragment;
import com.radiohyrule.android.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ListenFragment extends BaseFragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_listen, container, false);
    }

    @Override
    public String getTitle() {
        return "Listen";
    }
}
