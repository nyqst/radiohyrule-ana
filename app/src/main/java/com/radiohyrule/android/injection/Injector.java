package com.radiohyrule.android.injection;

public class Injector {

    private static AppComponent component;

    public static void setComponent(AppComponent value) {
        component = value;
    }

    public static AppComponent getComponent() { return component; }
}
