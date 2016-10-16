package devs.goldenpie.com.videowatchface.modules;

import android.util.Log;

import com.google.android.gms.wearable.DataMap;
import com.mariux.teleport.lib.TeleportClient;
import com.watchfacelib.Constant;

import org.apache.commons.lang3.ArrayUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import devs.goldenpie.com.videowatchface.event.FileStoredEvent;
import devs.goldenpie.com.videowatchface.model.BytesPart;
import devs.goldenpie.com.videowatchface.model.DataModel;
import pl.droidsonroids.gif.GifDrawable;

public class SortAndStore extends TeleportClient.OnSyncDataItemCallback {
    private static final String TAG = "SortAndStore";
    private TeleportClient mTeleportClient;
    private int iteration = 0;
    private List<DataModel> dataModelsList = new ArrayList<>();

    public SortAndStore(TeleportClient teleportClient) {
        mTeleportClient = teleportClient;
    }

    @Override
    public void onDataSync(DataMap dataMap) {
        organizeData(dataMap);
        mTeleportClient.setOnSyncDataItemCallback(this);
    }

    //    @DebugLog
    public void organizeData(DataMap dataMap) {
        Log.i(TAG, String.valueOf(Calendar.getInstance().getTimeInMillis()));
        Log.i(TAG, "Organize video file begin");

        Log.i(TAG, dataMap.toString());

        String description = dataMap.getString(Constant.VIDEO_DESCRIPTION);
        String author = dataMap.getString(Constant.VIDEO_AUTHOR);
        byte[] partOfData = dataMap.getByteArray(Constant.BYTE_ARRAY_PART);
        int partNum = dataMap.getInt(Constant.BYTE_ARRAY_PART_NUMBER);
        int fullPart = dataMap.getInt(Constant.VIDEO_FULL_PARTS);
        boolean containsVideo = false;

        BytesPart bp = new BytesPart();
        bp.setPosition(partNum);
        bp.setBytes(partOfData);

        if (!dataModelsList.isEmpty()) {
            for (int i = 0; i < dataModelsList.size(); i++) {
                if (dataModelsList.get(i).getDescription().equals(description)) {
                    if (author != null)
                        dataModelsList.get(i).setAuthor(author);
                    dataModelsList.get(i).getListOfBytes().add(bp);
                    containsVideo = true;
                    break;
                } else {
                    containsVideo = false;
                }
            }
        }

        if (!containsVideo) {
            DataModel dataModel = new DataModel();
            if (author != null)
                dataModel.setAuthor(author);
            dataModel.setDescription(description);
            dataModel.setFullPart(fullPart);
            dataModel.getListOfBytes().add(bp);
            dataModelsList.add(dataModel);
        }

        Log.i(TAG, String.valueOf(Calendar.getInstance().getTimeInMillis()));
        try {
            if (!dataModelsList.isEmpty()) {
                if (dataModelsList.get(iteration).getListOfBytes().size() == dataModelsList.get(iteration).getFullPart()) {
                    Log.i(TAG, "Organize video file end");
                    makeGif(dataModelsList.get(iteration));
                    iteration++;
                } else if (dataModelsList.get(iteration).getListOfBytes().size() != dataModelsList.get(iteration).getFullPart()
                        && dataModelsList.size() > iteration + 1
                        && dataModelsList.get(iteration + 1).getListOfBytes().size() == dataModelsList.get(iteration + 1).getFullPart()) {
                    iteration++;
                }
            }
        } catch (IndexOutOfBoundsException e) {
            iteration--;
        }

    }


    //    @DebugLog
    private void makeGif(final DataModel model) {
        new Thread(() -> {
            Log.i(TAG, "Make video file begin");

            ArrayList<BytesPart> bytesPartArrayList = model.getListOfBytes();
            byte[] bytes = new byte[0];

            sortBytes(bytesPartArrayList);

            for (int i = 0; i < bytesPartArrayList.size(); i++) {
                try {
                    bytes = ArrayUtils.addAll(bytes, bytesPartArrayList.get(i).getBytes());
                } catch (OutOfMemoryError e) {
                    e.printStackTrace();
                    return;
                }
            }

            Log.i(TAG, "Make video file end");

            try {
                storeGifFile(bytes, model);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

    }

    //    @DebugLog
    private void storeGifFile(byte[] bytes, DataModel model) throws IOException {
        Log.i(TAG, "Store video file begin");

        model.setGifDrawable(new GifDrawable(bytes));

        Log.i(TAG, "Store video file end");
        Log.i(TAG, "Post event begin");
        if (model.getGifDrawable() != null)
            EventBus.getDefault().post(new FileStoredEvent(model));
        Log.i(TAG, "Post event end");
    }

    public void sortBytes(ArrayList<BytesPart> bytesPartArrayList) {
        Collections.sort(bytesPartArrayList, (bytesPart, bytesPart2) -> bytesPart.getPosition() - bytesPart2.getPosition());
    }
}
