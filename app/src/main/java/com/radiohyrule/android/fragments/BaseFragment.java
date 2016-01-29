package com.radiohyrule.android.fragments;

import android.support.v4.app.Fragment;

import com.radiohyrule.android.activities.MainActivity;

public abstract class BaseFragment extends Fragment {
    public MainActivity getMainActivity() { return (MainActivity)getActivity(); }

    public Object getTitle() { return getTitleText(); }
    abstract public String getTitleText();
}
