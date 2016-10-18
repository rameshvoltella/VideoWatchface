package devs.goldenpie.com.videowatchface.service;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;

import com.constants.Constants;
import com.google.android.gms.wearable.DataMap;
import com.mariux.teleport.lib.TeleportClient;

import org.apache.commons.io.FilenameUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import devs.goldenpie.com.videowatchface.R;
import devs.goldenpie.com.videowatchface.model.WatchFaceSenderEvent;

/**
 * Created by EvilDev on 17.10.2016.
 */
public class ShareService {

    private static final String GIF_AUTHOR = "Gif author";
    private static final String TIME_STAMP = "time_stamp";

    private static TeleportClient mTeleportClient;
    private static ProgressDialog mProgressDialog;

    @SuppressLint("StaticFieldLeak")
    private static Context context;

    private ShareService(TeleportClient client, Context context) {
        ShareService.context = context;
        mTeleportClient = client;
    }

    public static ShareService getInstance(TeleportClient client, Context context) {
        return new ShareService(client, context);
    }

    public static void sendGif(String path) throws IOException {
        mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage(context.getString(R.string.uploading_watchface));
        RandomAccessFile f = new RandomAccessFile(path, "r");
        byte[] b = new byte[(int) f.length()];
        f.readFully(b);

        performArraySlick(b, path);
    }

    private static void performArraySlick(final byte[] bytes, String path) {
        if (!mProgressDialog.isShowing())
            mProgressDialog.show();

        ArrayList<DataMap> dataMaps = new ArrayList<>();

        double ratio = 1.0 * bytes.length / Constants.DEFAULT_VIDEO_PART_SIZE;
        int fullPart = (int) ratio;
        byte[] b;

        for (int i = 0; i < fullPart; i++) {
            b = new byte[Constants.DEFAULT_VIDEO_PART_SIZE];
            System.arraycopy(bytes, i * Constants.DEFAULT_VIDEO_PART_SIZE, b, 0, Constants.DEFAULT_VIDEO_PART_SIZE);

            DataMap d = sendBytes(i + 1, fullPart + 1, b, null, FilenameUtils.getBaseName(path));
            dataMaps.add(d);
        }

        if (ratio - fullPart != 0.0) {
            b = new byte[bytes.length - fullPart * Constants.DEFAULT_VIDEO_PART_SIZE];
            System.arraycopy(bytes, fullPart * Constants.DEFAULT_VIDEO_PART_SIZE, b, 0, b.length);

            DataMap d = sendBytes(fullPart + 1, fullPart + 1, b, GIF_AUTHOR, FilenameUtils.getBaseName(path));
            dataMaps.add(d);
        }

        new Handler().postDelayed(() -> {
            mProgressDialog.dismiss();
            EventBus.getDefault().post(new WatchFaceSenderEvent(path));
        }, (dataMaps.size() - 1) * 1000);

        final int[] i = {0};

        if (mTeleportClient.getGoogleApiClient().isConnected())
            mTeleportClient.syncAll(dataMaps.get(i[0]));

        mTeleportClient.setListener(() -> {
            i[0]++;
            if (i[0] <= dataMaps.size() - 1) {
                if (mTeleportClient.getGoogleApiClient().isConnected())
                    mTeleportClient.syncAll(dataMaps.get(i[0]));
            }
        });
    }

    private static DataMap sendBytes(int partNum, int fullPart, byte[] partOfBytes, String author, String description) {
        DataMap d = new DataMap();
        d.putByteArray(Constants.BYTE_ARRAY_PART, partOfBytes);
        d.putInt(Constants.BYTE_ARRAY_PART_NUMBER, partNum);
        d.putString(Constants.VIDEO_DESCRIPTION, description);
        d.putString(Constants.VIDEO_AUTHOR, author);
        d.putInt(Constants.VIDEO_FULL_PARTS, fullPart);
        d.putLong(TIME_STAMP, System.currentTimeMillis());

        return d;
    }
}