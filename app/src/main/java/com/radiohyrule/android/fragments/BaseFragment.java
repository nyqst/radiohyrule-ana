package com.radiohyrule.android.fragments;

import android.app.Fragment;

import com.radiohyrule.android.activities.MainActivity;

public abstract class BaseFragment extends Fragment {
    public MainActivity getMainActivity() { return (MainActivity)getActivity(); }

    public String getTitle() { return getTitleText(); }
    abstract public String getTitleText();
}
