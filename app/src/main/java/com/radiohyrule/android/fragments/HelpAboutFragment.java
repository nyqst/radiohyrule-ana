package com.radiohyrule.android.fragments;

import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.RawRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.radiohyrule.android.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class HelpAboutFragment extends Fragment {

    private static final String EXTRA_RAW_RESID = "HelpAboutFragment.RAW_RESID";
    WebView webView;

    public static HelpAboutFragment newInstance(@RawRes int resId) {
        HelpAboutFragment fragment = new HelpAboutFragment();
        Bundle args = new Bundle();
        args.putInt(EXTRA_RAW_RESID, resId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_webview, container, false);
        webView = rootView.findViewById(R.id.webview);
        String body = "";
        if(getArguments() != null) {
            int resId = getArguments().getInt(EXTRA_RAW_RESID);
            body = readTextFromResource(resId);
        }

        webView.getSettings().setSupportZoom(false);
        webView.setBackgroundColor(Color.LTGRAY);
        webView.getSettings().setBuiltInZoomControls(false);
        webView.getSettings().setDisplayZoomControls(false);
        webView.loadDataWithBaseURL(null, body, "text/html", "UTF-8", null);

        return rootView;
    }

    private String readTextFromResource(int resourceID) {
        InputStream raw = getResources().openRawResource(resourceID);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        int i;
        try {
            i = raw.read();
            while (i != -1) {
                stream.write(i);
                i = raw.read();
            }
            raw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stream.toString();
    }
}
