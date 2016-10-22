package com.goldenpie.devs.videowatchface.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * @author anton
 * @version 3.4
 * @since 18.10.16
 */
public class ApplicationPreference {

    public static final String GIF_PATH = "gif_path";
    private final SharedPreferences preferences;

    public ApplicationPreference(Context context) {
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public String getCurrentGif() {
        return preferences.getString(GIF_PATH, "");
    }


    public void setCurrentGif(String path) {
        preferences.edit()
                .putString(GIF_PATH, path)
                .apply();
    }
}
