/*
 * Util
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Jeffrey Glenn on 27 Feb 2014
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.cast.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;

import com.example.cast.service.capability.listeners.ErrorListener;
import com.example.cast.service.capability.listeners.ResponseListener;
import com.example.cast.service.command.ServiceCommandError;

import org.apache.http.conn.util.InetAddressUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public final class Util {
    public static String T = "Connect SDK";
    private static Executor executor;
    private static Handler handler;

    static {
        createExecutor();
    }

    static void createExecutor() {
        executor = Executors.newFixedThreadPool(20, new ThreadFactory() {
            /* class com.inshot.cast.core.core.Util.ThreadFactoryC56811 */

            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable);
                thread.setName("2nd Screen BG");
                return thread;
            }
        });
    }

    public static void runOnUI(Runnable runnable) {
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
        handler.post(runnable);
    }

    public static void runInBackground(Runnable runnable, boolean z) {
        if (runnable != null) {
            if (z || isMain()) {
                executor.execute(runnable);
            } else {
                runnable.run();
            }
        }
    }

    public static void runInBackground(Runnable runnable) {
        runInBackground(runnable, false);
    }

    public static Executor getExecutor() {
        return executor;
    }

    public static boolean isMain() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public static <T> void postSuccess(final ResponseListener<T> responseListener, final T t) {
        if (responseListener != null) {
            runOnUI(new Runnable() {
                /* class com.inshot.cast.core.core.Util.RunnableC56822 */

                public void run() {
                    responseListener.onSuccess(t);
                }
            });
        }
    }

    public static void postError(final ErrorListener errorListener, final ServiceCommandError serviceCommandError) {
        if (errorListener != null) {
            runOnUI(new Runnable() {
                /* class com.inshot.cast.core.core.Util.RunnableC56833 */

                public void run() {
                    errorListener.onError(serviceCommandError);
                }
            });
        }
    }

    public static byte[] convertIpAddress(int i) {
        return new byte[]{(byte) (i & 255), (byte) ((i >> 8) & 255), (byte) ((i >> 16) & 255), (byte) ((i >> 24) & 255)};
    }

    public static long getTime() {
        return TimeUnit.MILLISECONDS.toSeconds(new Date().getTime());
    }

    public static boolean isIPv4Address(String str) {
        return InetAddressUtils.isIPv4Address(str);
    }

    public static boolean isIPv6Address(String str) {
        return InetAddressUtils.isIPv6Address(str);
    }

    public static InetAddress getIpAddress(Context context) throws UnknownHostException {
        int ipAddress;
        @SuppressLint("WrongConstant") WifiInfo connectionInfo = ((WifiManager) context.getSystemService("wifi")).getConnectionInfo();
        if (connectionInfo == null || (ipAddress = connectionInfo.getIpAddress()) == 0) {
            return null;
        }
        return InetAddress.getByAddress(convertIpAddress(ipAddress));
    }

    public static String getSuffix(String str) {
        int lastIndexOf;
        return (str == null || !str.contains(".") || (lastIndexOf = str.lastIndexOf(".")) < 0) ? "" : str.substring(lastIndexOf + 1);
    }
}