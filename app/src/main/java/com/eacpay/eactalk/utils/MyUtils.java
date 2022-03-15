package com.eacpay.eactalk.utils;

import android.os.Environment;
import android.util.Log;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MyUtils {
    private static final String TAG = "oldfeel";
    static String imageExtArray[] = {"bmp", "dib", "gif", "jfif", "jpe", "jpeg", "jpg", "png", "tif", "tiff", "ico"};
    static String videoExtArray[] = {"mp4"};

    public static boolean isImageByName(String fileName) {
        if (fileName == null) {
            return false;
        }
        for (int i = 0; i < imageExtArray.length; i++) {
            if (fileName.toLowerCase().endsWith("." + imageExtArray[i])) {
                return true;
            }
        }
        return false;
    }

    public static boolean isVideoByName(String fileName) {
        ContentInfoUtil util = new ContentInfoUtil();
        ContentInfo info = util.findExtensionMatch(fileName);
        return info.getMimeType().startsWith("video");
    }


    public static String guessMimeType(byte[] data) {
        try {
            ContentInfoUtil util = new ContentInfoUtil();
            ContentInfo info = util.findMatch(data);
            return info.getMimeType();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static File saveBytesToFile(byte[] data, String name) {
        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/eactalk/");
        dir.mkdirs();
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/eactalk/" + name);
        Log.e(TAG, "saveBytesToFile: " + file.getAbsolutePath());
        BufferedOutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(file));
            outputStream.write(data);
            outputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "saveBytesToFile: " + e.getMessage());
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return file;
    }

    public static String getTimeStamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    public static String formatTimeL(long timeStamp) {
        Date currentLocalTime = new Date(timeStamp == 0 ? System.currentTimeMillis() : timeStamp * 1000);

        SimpleDateFormat date1 = new SimpleDateFormat("MM/dd", Locale.getDefault());
        SimpleDateFormat date2 = new SimpleDateFormat("HH:mm", Locale.getDefault());
        if (System.currentTimeMillis() - currentLocalTime.getTime() > 24 * 60 * 60 * 1000) {
            return date1.format(currentLocalTime);
        }

        return date2.format(currentLocalTime);
    }

    /**
     * yyyy/MM/dd HH:mm:ss
     *
     * @param timeStamp
     * @return
     */
    public static String formatTimeLA(long timeStamp) {
        Date currentLocalTime = new Date(timeStamp == 0 ? System.currentTimeMillis() : timeStamp * 1000);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());

        return dateFormat.format(currentLocalTime);
    }
}
