package devs.goldenpie.com.videowatchface.ui.activity;

import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import devs.goldenpie.com.videowatchface.R;

/**
 * Created by EvilDev on 14.10.2016.
 */
public abstract class BaseActivity extends AppCompatActivity {

    void requestPermission(final String permission, String rationale, final int requestCode) {
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
}
