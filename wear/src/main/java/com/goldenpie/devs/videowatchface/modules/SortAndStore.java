package com.goldenpie.devs.videowatchface.modules;

import android.os.AsyncTask;
import android.util.Log;

import com.constants.Constants;
import com.goldenpie.devs.videowatchface.event.FileStoredEvent;
import com.goldenpie.devs.videowatchface.model.BytesPart;
import com.goldenpie.devs.videowatchface.model.DataModel;
import com.goldenpie.devs.videowatchface.model.db.FileModel;
import com.google.android.gms.wearable.DataMap;
import com.hwangjr.rxbus.RxBus;

import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class SortAndStore{
    private static final String TAG = "SortAndStore";
    private int iteration = 0;
    private List<DataModel> dataModelsList = new ArrayList<>();

    public void organizeData(DataMap dataMap) {
        Log.i(TAG, String.valueOf(Calendar.getInstance().getTimeInMillis()));
        Log.i(TAG, "Organize video file begin");

        Log.i(TAG, dataMap.toString());

        String description = dataMap.getString(Constants.GIF_PATH);
        String author = dataMap.getString(Constants.GIF_INDIFICATOR);
        byte[] partOfData = dataMap.getByteArray(Constants.BYTE_ARRAY_PART);
        int partNum = dataMap.getInt(Constants.BYTE_ARRAY_PART_NUMBER);
        int fullPart = dataMap.getInt(Constants.GIF_FULL_PARTS);
        boolean containsVideo = false;

        BytesPart bp = new BytesPart();
        bp.setPosition(partNum);
        bp.setBytes(partOfData);

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

    private void makeGif(final DataModel model) {
        AsyncTask.execute(() -> {
            Log.i(TAG, "Make video file begin");

            ArrayList<BytesPart> bytesPartArrayList = model.getListOfBytes();
            dataModelsList.clear();
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
                storeGifFile(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    //    @DebugLog
    private void storeGifFile(byte[] bytes) throws IOException {
        Log.i(TAG, "Store video file begin");

        FileModel fileModel;
        if (FileModel.isExist()) {
            fileModel = FileModel.getFileModel();
        } else {
            fileModel = new FileModel();
        }
        fileModel.setData(bytes);
        fileModel.save();

        Log.i(TAG, "Store video file end");
        Log.i(TAG, "Post event begin");
        RxBus.get().post(new FileStoredEvent(fileModel));
        Log.i(TAG, "Post event end");
    }

    private void sortBytes(ArrayList<BytesPart> bytesPartArrayList) {
        Collections.sort(bytesPartArrayList, (bytesPart, bytesPart2) -> bytesPart.getPosition() - bytesPart2.getPosition());
    }
}
