package com.iotconnectsdk.utils;

import android.app.Activity;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.iotconnectsdk.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class IotSDKLogUtils {

    private static IotSDKLogUtils iotSDKLogUtils;
    private String cpId;
    private String uniqueId;
    private Context activity;

    public static IotSDKLogUtils getInstance(Context act, String cpId, String uniqueId) {
        if (iotSDKLogUtils == null) return new IotSDKLogUtils(act, cpId, uniqueId);
        return iotSDKLogUtils;
    }

    private IotSDKLogUtils(Context act, String cpId, String uniqueId) {
        this.activity = act;
        this.cpId = cpId;
        this.uniqueId = uniqueId;
    }

    public void log(boolean isError, boolean isDebug, String code, String msg) {
        if (isDebug) {

            String currentDate = IotSDKUtils.getCurrentDate();

            String LOG = "[" + code + "] " + currentDate + " [" + this.cpId + "_" + this.uniqueId + "]: " + msg;
         //   Log.d("", LOG);

            writeLogFile(isError, LOG);
        }
    }

    public void writeLogFile(boolean isError, String log) {

        try {
//            File errorFile = new File("sdcard/IotSDK/logs/debug", "error.txt");
//            File infoFile = new File("sdcard/IotSDK/logs/debug", "info.txt");

            String directoryPath = "logs/debug";
            File directory = getChildrenFolder(directoryPath);
            File errorFile = new File(directory, "error.txt");
            File infoFile = new File(directory, "info.txt");

            if (isError)
                checkFile(errorFile);
            else
                checkFile(infoFile);

            if (isError)
                writeToFile(errorFile, log);
            else
                writeToFile(infoFile, log);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writePublishedMessage(String directoryPath, String textFile, String message) {

        File directory = getChildrenFolder(directoryPath);
        File file = new File(directory, textFile + ".txt");

        checkFile(file);

        writeToFile(file, message);

    }

    private File getChildrenFolder(String path) {
        File dir = activity.getFilesDir();
        List<String> dirs = new ArrayList<String>(Arrays.<String>asList(path.split("/")));
        for (int i = 0; i < dirs.size(); ++i) {
            dir = new File(dir, dirs.get(i)); //Getting a file within the dir.
            if (!dir.exists()) {
                dir.mkdir();
            }
        }
        return dir;
    }

    private void checkFile(File file) {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();

            } catch (IOException e) {
                // log(false, true, "0000", e.getMessage());
                e.printStackTrace();
            }
        }
    }


    private void writeToFile(File file, String log) {

        try {
            // Adds a line to the file
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
            writer.write(log);
            writer.newLine();
            writer.close();
        } catch (Exception e) {
            //Log.d("writeToFile", e.getMessage());
            e.printStackTrace();
        }
    }
}
