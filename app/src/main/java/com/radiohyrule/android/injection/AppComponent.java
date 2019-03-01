package com.radiohyrule.android.injection;

import com.radiohyrule.android.HyruleApplication;
import com.radiohyrule.android.player.ExoService;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = AppModule.class)
public interface AppComponent {

    void inject(HyruleApplication hyruleApplication);

    void inject(ExoService exoService);
}
