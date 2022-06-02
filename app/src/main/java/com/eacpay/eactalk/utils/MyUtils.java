package com.eacpay.eactalk.utils;

import static com.eacpay.eactalk.ipfs.IpfsDataFetcher.ENCRYPT_PREFIX;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import com.blankj.utilcode.util.EncryptUtils;
import com.eacpay.R;
import com.eacpay.eactalk.fragment.main.ContactFragment.ContactItem;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.google.gson.Gson;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyUtils {
    private static final String TAG = "oldfeel";
    static String[] imageExtArray = {"bmp", "dib", "gif", "jfif", "jpe", "jpeg", "jpg", "png", "tif", "tiff", "ico"};
    static String[] videoExtArray = {"mp4"};

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
        ContentInfo info = ContentInfoUtil.findExtensionMatch(fileName);
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

    public static File saveBytesToFile(Context context, byte[] data, String name) {
        String path = PreferenceManager.getDefaultSharedPreferences(context).getString("path", defaultIPFSPath());
        File dir = new File(path);
        dir.mkdirs();
        File file = new File(path + "/" + name);
        if (file != null && file.exists()) {
            return file;
        }
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

    public static boolean isEmpty(String name) {
        return name == null || name.equals("");
    }

    public static void copy(Context context, String text) {
        //获取剪贴板管理器：
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        // 创建普通字符型ClipData
        ClipData mClipData = ClipData.newPlainText("Label", text);
        // 将ClipData内容放到系统剪贴板里。
        cm.setPrimaryClip(mClipData);
    }

    public static void exportContact(FragmentActivity activity) {
        List<ContactItem> list = BRSharedPrefs.getContactList(activity);
        File file = saveBytesToFile(activity, new Gson().toJson(list).getBytes(), "contact.json");
        Toast.makeText(activity, activity.getString(R.string.export_success_) + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
    }

    public static String getFileName(Activity activity, Uri returnUri) {
        Cursor returnCursor = activity.getContentResolver().query(returnUri, null, null, null, null);
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();
        String fileName = returnCursor.getString(nameIndex);
        return fileName;
    }


    public static byte[] getFileBytes(Activity activity, Uri uri) {
        try {
            InputStream inputStream = activity.getContentResolver().openInputStream(uri);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];

            int len = 0;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            return byteBuffer.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void log(String msg) {
        Log.e(TAG, "log: " + msg);
    }

    public static String defaultIPFSPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/eactalk/";
    }

    public static String encrypt(String data, String addressTo) {
        String key = getEncryptKey(addressTo);
        return EncryptUtils.encryptDES2HexString(data.getBytes(), key.getBytes(), "DES", key.getBytes());
    }

    private static String getEncryptKey(String addressTo) {
        if (addressTo == null || addressTo.length() < 6) {
            return "";
        }
        String key = EncryptUtils.encryptMD5ToString((addressTo.substring(0, 3) + addressTo.substring(addressTo.length() - 6, addressTo.length() - 1)).getBytes());
        return key;
    }

    public static byte[] encryptFile(byte[] data, String addressTo) {
        String key = getEncryptKey(addressTo);
        return EncryptUtils.encryptDES(data, key.getBytes(), "DES", key.getBytes());
    }

    public static String decrypt(String data, String addressTo) {
        String key = getEncryptKey(addressTo);
        data = data.replace(ENCRYPT_PREFIX, "");
        MyUtils.log("data is " + data);
        MyUtils.log("key is " + key);
        byte[] result = EncryptUtils.decryptHexStringDES(data, key.getBytes(), "DES", key.getBytes());
        if (result == null) {
            log("result is null");
            return "";
        }
        return new String(result);
    }

    public static byte[] decryptFile(byte[] data, String addressTo) {
        String key = getEncryptKey(addressTo);
        return EncryptUtils.decryptDES(data, key.getBytes(), "DES", key.getBytes());
    }

    /**
     * 判断是否是 ipfs 的 cid
     *
     * @param txIPFS
     * @return
     */
    public static boolean isIPFSCID(String txIPFS) {
        if (txIPFS == null || txIPFS.length() != 46) {
            return false;
        }
        return true;
    }
}
