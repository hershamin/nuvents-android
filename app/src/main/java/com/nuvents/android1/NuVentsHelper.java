package com.nuvents.android1;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.text.TextUtils;

import org.apache.http.util.ByteArrayBuffer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;

/**
 * Created by hersh on 7/29/15.
 */

public class NuVentsHelper {

    // Get MD5SUM of file
    public static String getMD5SUM(String filePath) {

        String md5sum = "";
        try {
            FileInputStream fis = new FileInputStream(filePath);
            char[] hexDigits = "0123456789abcdef".toCharArray();
            int read = 0;
            byte[] bytes = new byte[4096];

            MessageDigest digest = MessageDigest.getInstance("MD5");
            while ((read = fis.read(bytes)) != -1) {
                digest.update(bytes, 0, read);
            }
            byte[] messageDigest = digest.digest();

            StringBuilder sb = new StringBuilder(32);
            for (byte b : messageDigest) {
                sb.append(hexDigits[(b >> 4) & 0x0f]);
                sb.append(hexDigits[b & 0x0f]);
            }
            md5sum += sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return md5sum;
    }

    // Function to download from web & save
    public static void downloadFile(String filePath, String url) {
        try {
            URL urlU = new URL(url);
            File file = new File(filePath);
            URLConnection uConn = urlU.openConnection();

            InputStream is = uConn.getInputStream();
            BufferedInputStream bufferInStream = new BufferedInputStream(is);

            ByteArrayBuffer baf = new ByteArrayBuffer(5000);
            int current = 0;
            while ((current = bufferInStream.read()) != -1) {
                baf.append((byte) current);
            }

            FileOutputStream fos =new FileOutputStream(file);
            fos.write(baf.toByteArray());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Get device hardware type
    public static String getDeviceHardware() {
        final String manufacturer = Build.MANUFACTURER;
        final String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }
        if (manufacturer.equalsIgnoreCase("HTC")) {
            // make sure "HTC" is fully capitalized.
            return  "HTC " + model;
        }
        return capitalize(manufacturer) + " " + model;
    }

    // Helper function for getting device hardware type
    private static String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        final char[] arr = str.toCharArray();
        boolean capitalizeNext = true;
        String phrase = "";
        for (final char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase += Character.toUpperCase(c);
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase += c;
        }
        return phrase;
    }

    // Resize image based on width
    public static Bitmap resizeImage(Bitmap origImage, float width) {

        // Resize image to required width
        int oldWidth = origImage.getWidth();
        float scaleFactor = width / oldWidth;
        int newHeight = (int)(origImage.getHeight() * scaleFactor);
        int newWidth = (int)(oldWidth * scaleFactor);
        Bitmap newImage = Bitmap.createScaledBitmap(origImage, newWidth, newHeight, true);

        return newImage;
    }

    // Get resource from internal file system
    public static String getResourcePath(String resource, String type, Context context) {
        // Create directories if not present
        String mainDirS = context.getFilesDir().getPath() + "/resources/" + type;
        File mainDir = new File(mainDirS);
        if (!mainDir.exists()) {
            mainDir.mkdirs();
        }
        // Return file path
        String filePath = mainDirS + "/" + resource;
        return filePath;
    }

}
