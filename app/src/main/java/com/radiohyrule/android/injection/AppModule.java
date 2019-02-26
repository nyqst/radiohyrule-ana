package com.radiohyrule.android.injection;

import android.content.Context;

import com.radiohyrule.android.HyruleApplication;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(includes = {ApiModule.class})
public class AppModule {

    private HyruleApplication application;

    public AppModule(HyruleApplication application) {
        this.application = application;
    }

    @Provides @Singleton
    public Context providesContext() {
        return application;
    }

    @Provides @Singleton
    public HyruleApplication providesApplication() {
        return application;
    }
}
