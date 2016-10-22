package com.goldenpie.devs.videowatchface;

import android.app.Application;

import com.activeandroid.ActiveAndroid;

/**
 * @author anton
 * @version 3.4
 * @since 18.10.16
 */
public class WearApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ActiveAndroid.initialize(this);
    }
}
