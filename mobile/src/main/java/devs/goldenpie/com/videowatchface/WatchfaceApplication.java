package devs.goldenpie.com.videowatchface;

import android.app.Application;

import com.activeandroid.ActiveAndroid;

/**
 * Created by EvilDev on 14.10.2016.
 */
public class WatchfaceApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ActiveAndroid.initialize(this);
    }
}
