package com.radiohyrule.android;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.squareup.picasso.Picasso;

import io.fabric.sdk.android.Fabric;

public class Application extends android.app.Application{
    @Override
    public void onCreate() {
        //initialization code goes here
        super.onCreate();

        // Set up Crashlytics, disabled for debug builds
        Crashlytics crashlyticsKit = new Crashlytics.Builder()
                .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                .build();

        // Initialize Fabric with the debug-disabled crashlytics.
        Fabric.with(this, crashlyticsKit);

        Picasso.with(this).setIndicatorsEnabled(BuildConfig.DEBUG);
        //Picasso.with(this).setLoggingEnabled(true); //Do not leave enabled, not even for debug builds. Local temp use only
    }
}
