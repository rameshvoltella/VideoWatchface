package devs.goldenpie.com.videowatchface.service;

import com.constants.Constants;
import com.google.android.gms.wearable.DataMap;
import com.mariux.teleport.lib.TeleportClient;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;

/**
 * Created by EvilDev on 17.10.2016.
 */
public class ShareService {

    public static final String GIF_DESCRIPTION = "Gif description";
    public static final String GIF_AUTHOR = "Gif author";
    public static final String TIME_STAMP = "time_stamp";

    private static TeleportClient mTeleportClient;

    public ShareService(TeleportClient client) {
        mTeleportClient = client;
    }

    public static ShareService getInstance(TeleportClient client) {
        return new ShareService(client);
    }

    public static void sendGif(String path) throws IOException {
        RandomAccessFile f = new RandomAccessFile(path, "r");
        byte[] b = new byte[(int) f.length()];
        f.readFully(b);

        performArraySlick(b);
    }

    private static void performArraySlick(final byte[] bytes) {
        double ratio = 1.0 * bytes.length / Constants.DEFAULT_VIDEO_PART_SIZE;
        int fullPart = (int) ratio;
        byte[] b;
        for (int i = 0; i < fullPart; i++) {
            b = new byte[Constants.DEFAULT_VIDEO_PART_SIZE];
            System.arraycopy(bytes, i * Constants.DEFAULT_VIDEO_PART_SIZE, b, 0, Constants.DEFAULT_VIDEO_PART_SIZE);

            sendBytes(i + 1, fullPart + 1, b, null, GIF_DESCRIPTION);
        }

        if (ratio - fullPart != 0.0) {
            b = new byte[bytes.length - fullPart * Constants.DEFAULT_VIDEO_PART_SIZE];
            System.arraycopy(bytes, fullPart * Constants.DEFAULT_VIDEO_PART_SIZE, b, 0, b.length);
            sendBytes(fullPart + 1, fullPart + 1, b, GIF_AUTHOR, GIF_DESCRIPTION);
        }
    }

    private static void sendBytes(int partNum, int fullPart, byte[] partOfBytes, String author, String description) {
        DataMap d = new DataMap();
        d.putByteArray(Constants.BYTE_ARRAY_PART, partOfBytes);
        d.putInt(Constants.BYTE_ARRAY_PART_NUMBER, partNum);
        d.putString(Constants.VIDEO_DESCRIPTION, description);
        d.putString(Constants.VIDEO_AUTHOR, author);
        d.putInt(Constants.VIDEO_FULL_PARTS, fullPart);
        d.putLong(TIME_STAMP, new Date().getTime());
        if (mTeleportClient.getGoogleApiClient().isConnected())
            mTeleportClient.syncAll(d);

    }
}
