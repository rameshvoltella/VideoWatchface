package com.goldenpie.devs.videowatchface.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ShareEvent;
import com.goldenpie.devs.videowatchface.BuildConfig;
import com.goldenpie.devs.videowatchface.R;

import butterknife.ButterKnife;

/**
 * Created by EvilDev on 14.10.2016.
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected abstract int getContentView();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentView());
        ButterKnife.bind(this);
    }

    protected void requestPermission(final String permission, String rationale, final int requestCode) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.permission_title_rationale));
            builder.setMessage(rationale);
            builder.setPositiveButton(getString(android.R.string.ok), (dialog, which) -> {
                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            });
            builder.setNegativeButton(getString(android.R.string.cancel), null);
            builder.show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
        }
    }

    protected void showShareDialog() {
        String message = String.format(getString(R.string.sahre_content),
                getString(R.string.app_name),
                "https://play.google.com/store/apps/details?id=devs.goldenpie.com.videowatchface");
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, message);

        startActivity(Intent.createChooser(share, getString(R.string.share_link)));

        if (!BuildConfig.DEBUG)
            Answers.getInstance().logShare(new ShareEvent()
                    .putMethod("LINK_SHARE"));
    }

}
