package com.lygttpod.monitor.utils;


import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {

    public static boolean copyFileToAppDirectory(String sourcePath, String destPath) {
        try {
            File sourceFile = new File(sourcePath);
            File destFile = new File(destPath);

            // 确保目标文件不在源文件夹内，避免无限循环
            if (isSubdirectory(sourceFile, destFile)) {
                throw new IOException("Destination cannot be inside source directory");
            }

            copyFileContent(sourceFile, destFile);
            return true;
        } catch (IOException e) {
            Log.e("FileUtils", "Error copying file", e);
            return false;
        }
    }

    private static void copyFileContent(File source, File dest) throws IOException {
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(dest)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    // 检查一个目录是否是另一个目录的子目录
    private static boolean isSubdirectory(File parent, File child) {
        String parentPath = parent.getAbsolutePath();
        String childPath = child.getAbsolutePath();
        return childPath.startsWith(parentPath);
    }
}

