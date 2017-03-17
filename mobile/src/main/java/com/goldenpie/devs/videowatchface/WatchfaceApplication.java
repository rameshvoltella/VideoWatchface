package com.goldenpie.devs.videowatchface;

import android.support.multidex.MultiDexApplication;

import com.activeandroid.ActiveAndroid;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.goldenpie.devs.videowatchface.ui.activity.TrimmerActivity;
import com.goldenpie.devs.videowatchface.utils.DetectWear;

import java.io.File;

import io.fabric.sdk.android.Fabric;

/**
 * Created by EvilDev on 14.10.2016.
 */
public class WatchfaceApplication extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();

        ActiveAndroid.initialize(this);
        DetectWear.init(this);

        if (!BuildConfig.DEBUG)
            Fabric.with(this, new Crashlytics(), new Answers());

        createDir();
    }

    private void createDir() {
        File dir = new File(TrimmerActivity.DESTINATION_PATH);
        if (!dir.exists())
            dir.mkdir();
    }
}
