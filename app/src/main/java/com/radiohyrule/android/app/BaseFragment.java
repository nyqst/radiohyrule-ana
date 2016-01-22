package com.radiohyrule.android.app;

import android.support.v4.app.Fragment;

public abstract class BaseFragment extends Fragment {
    public MainActivity getMainActivity() { return (MainActivity)getActivity(); }

    public Object getTitle() { return getTitleText(); }
    abstract public String getTitleText();
}
