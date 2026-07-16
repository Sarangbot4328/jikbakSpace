package com.webtoonmap.mobile.storage;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.Locale;
import java.util.UUID;

public final class AvseeStorage {
    private AvseeStorage() { }

    public static File root(Context context) {
        File external = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        File root = external == null ? new File(context.getFilesDir(), "avsee") :
                new File(external, "avsee");
        if (!root.exists() && !root.mkdirs()) {
            root = new File(context.getFilesDir(), "avsee");
            if (!root.exists()) root.mkdirs();
        }
        return root;
    }

    public static File createVideoFolder(Context context, String title) {
        String safe = safeName(title);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        File folder = new File(root(context), safe + "_" + suffix);
        if (!folder.mkdirs()) throw new IllegalStateException("영상 저장 폴더를 만들 수 없습니다.");
        return folder;
    }

    public static void deleteVideoFiles(String filePath) {
        if (filePath == null || filePath.isEmpty()) return;
        File file = new File(filePath);
        File folder = file.getParentFile();
        if (folder == null || !folder.isDirectory()) return;
        deleteFolder(folder);
    }

    public static void cleanupIncomplete(Context context) {
        File[] folders = root(context).listFiles();
        if (folders == null) return;
        for (File folder : folders) {
            if (!folder.isDirectory()) continue;
            File[] children = folder.listFiles();
            if (children == null) continue;
            for (File child : children) {
                if (child.getName().endsWith(".part")) {
                    deleteFolder(folder);
                    break;
                }
            }
        }
    }

    public static void deleteFolder(File folder) {
        if (folder == null || !folder.exists()) return;
        File[] children = folder.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) deleteFolder(child);
                else child.delete();
            }
        }
        folder.delete();
    }

    public static String extensionFromUrl(String url) {
        if (url == null) return ".mp4";
        String clean = url.split("[?#]", 2)[0].toLowerCase(Locale.US);
        for (String ext : new String[]{".mp4", ".webm", ".m4v", ".mov"}) {
            if (clean.endsWith(ext)) return ext;
        }
        return ".mp4";
    }

    public static String formatBytes(long bytes) {
        if (bytes >= 1024L * 1024L * 1024L) {
            return String.format(Locale.KOREA, "%.1f GB", bytes / (1024d * 1024d * 1024d));
        }
        return String.format(Locale.KOREA, "%.1f MB", bytes / (1024d * 1024d));
    }

    private static String safeName(String title) {
        String value = title == null ? "avsee" : title.trim();
        value = value.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", " ");
        if (value.isEmpty()) value = "avsee";
        return value.substring(0, Math.min(value.length(), 54));
    }
}
