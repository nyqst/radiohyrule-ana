package com.radiohyrule.android;

import com.squareup.picasso.Picasso;

public class Application extends android.app.Application{
    @Override
    public void onCreate() {
        //initialization code goes here
        super.onCreate();

        Picasso.with(this).setIndicatorsEnabled(BuildConfig.DEBUG);
        //Picasso.with(this).setLoggingEnabled(true); //Do not leave enabled, not even for debug builds. Local temp use only
    }
}
