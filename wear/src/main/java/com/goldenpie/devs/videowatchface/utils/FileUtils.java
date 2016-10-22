package com.goldenpie.devs.videowatchface.utils;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by EvilDev on 17.10.2016.
 */
public class FileUtils {

    public static final String PATHNAME =  Environment.getExternalStorageDirectory() + File.separator + "watchface.gif";

    public static File writeByteToFile(byte[] bytes) throws IOException {
        File file = new File(PATHNAME);
        if (!file.exists())
            file.createNewFile();
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(bytes);
        fos.close();
        return file;
    }

    public static File getFile(){
        return new File(PATHNAME);
    }
}
