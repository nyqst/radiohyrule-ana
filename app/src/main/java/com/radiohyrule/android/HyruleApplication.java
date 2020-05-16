package com.radiohyrule.android;

//import com.crashlytics.android.Crashlytics;
//import com.crashlytics.android.core.CrashlyticsCore;
import com.radiohyrule.android.injection.AppModule;
import com.radiohyrule.android.injection.DaggerAppComponent;
import com.radiohyrule.android.injection.Injector;
import com.squareup.picasso.Picasso;

//import io.fabric.sdk.android.Fabric;

public class HyruleApplication extends android.app.Application{

    @Override
    public void onCreate() {
        super.onCreate();

        // Set up Crashlytics, disabled for debug builds
        //Crashlytics crashlyticsKit = new Crashlytics.Builder()
        //        .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
        //        .build();

        // Initialize Fabric with the debug-disabled crashlytics.
        //Fabric.with(this, crashlyticsKit);

        Injector.setComponent(DaggerAppComponent.builder().appModule(new AppModule(this)).build());
        Injector.getComponent().inject(this);

        Picasso.with(this).setIndicatorsEnabled(BuildConfig.DEBUG);
    }

}
