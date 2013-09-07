package com.radiohyrule.android.app;

import android.support.v4.app.Fragment;

public abstract class BaseFragment extends Fragment {
    public Object getTitle() { return getTitleText(); }
    abstract public String getTitleText();
}
