package io.particle.android.sdk.utils;

import android.support.v4.util.ArrayMap;
import android.util.Log;

/**
 * NOTE: this class is likely to be deprecated soon in favor of Jake Wharton's Timber:
 * https://goo.gl/xmQYYU
 */
public class TLog {

    private static final ArrayMap<Class<?>, TLog> loggers = new ArrayMap<>();

    public static TLog get(Class<?> clazz) {
        TLog logger = loggers.get(clazz);
        if (logger == null) {
            logger = new TLog(clazz.getSimpleName());
            loggers.put(clazz, logger);
        }
        return logger;
    }

    private final String tag;

    private TLog(String tag) {
        this.tag = tag;
    }

    public void e(String msg) {
        Log.e(tag, msg);
    }

    public void e(String msg, Throwable tr) {
        Log.e(tag, msg, tr);
    }

    public void w(String msg) {
        Log.w(tag, msg);
    }

    public void w(String msg, Throwable tr) {
        Log.w(tag, msg, tr);
    }

    public void i(String msg) {
        Log.i(tag, msg);
    }

    public void i(String msg, Throwable tr) {
        Log.i(tag, msg, tr);
    }

    public void d(String msg) {
        Log.d(tag, msg);
    }

    public void d(String msg, Throwable tr) {
        Log.d(tag, msg, tr);
    }

    public void v(String msg) {
        Log.v(tag, msg);
    }

    public void v(String msg, Throwable tr) {
        Log.v(tag, msg, tr);
    }

    public void wtf(String msg) {
        Log.wtf(tag, msg);
    }

    public void wtf(String msg, Throwable tr) {
        Log.wtf(tag, msg, tr);
    }

}
