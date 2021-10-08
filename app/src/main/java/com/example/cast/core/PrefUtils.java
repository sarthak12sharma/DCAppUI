package com.example.cast.core;

import android.content.SharedPreferences;

import com.example.cast.discovery.DiscoveryManager;

public class PrefUtils {
    public static void putInt(String str, int i) {
        if (getEdit() != null) {
            getEdit().putInt(str, i).apply();
        }
    }

    public static int getInt(String str, int i) {
        if (getPref() == null) {
            return 0;
        }
        return getPref().getInt(str, i);
    }

    public static void putBool(String str, boolean z) {
        if (getEdit() != null) {
            getEdit().putBoolean(str, z).apply();
        }
    }

    public static boolean getBool(String str, boolean z) {
        return getPref() != null && getPref().getBoolean(str, z);
    }

    public static long getLong(String str, long j) {
        if (getPref() == null) {
            return 0;
        }
        return getPref().getLong(str, j);
    }

    public static void putLong(String str, long j) {
        if (getEdit() != null) {
            getEdit().putLong(str, j).apply();
        }
    }

    public static void putString(String str, String str2) {
        if (getEdit() != null) {
            getEdit().putString(str, str2).apply();
        }
    }

    public static String getString(String str, String str2) {
        if (getPref() == null) {
            return null;
        }
        return getPref().getString(str, str2);
    }

    private static SharedPreferences getPref() {
        if (DiscoveryManager.getInstance().getContext() == null) {
            return null;
        }
        return DiscoveryManager.getInstance().getContext().getSharedPreferences("xcast_pref_info", 0);
    }

    private static SharedPreferences.Editor getEdit() {
        if (getPref() != null) {
            return getPref().edit();
        }
        return null;
    }
}
