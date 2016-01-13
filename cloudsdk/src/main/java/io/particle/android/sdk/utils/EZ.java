package io.particle.android.sdk.utils;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import java.io.Closeable;
import java.io.IOException;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Analgesic shortcuts for Android dev't.
 */
@ParametersAreNonnullByDefault
public class EZ {

    private static final TLog log = TLog.get(EZ.class);


    public static void runOnMainThread(Runnable runnable) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(runnable);
    }

    public static void runOnMainThreadDelayed(long delayInMillis, Runnable runnable) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(runnable, delayInMillis);
    }

    public static boolean isThisTheMainThread() {
        return Looper.getMainLooper() == Looper.myLooper();
    }

    public static void runAsync(final Runnable runnable) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                runnable.run();
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void threadSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            log.e("Thread interrupted: ", e);
        }
    }

    public static boolean isUsingOlderWifiStack() {
        return Build.VERSION.SDK_INT < 20;
    }

    /**
     * Return the callbacks for a fragment or throw an exception.
     * <p/>
     * Inspired by: https://gist.github.com/keyboardr/5455206
     */
    @SuppressWarnings("unchecked")
    public static <T> T getCallbacksOrThrow(Fragment frag, Class<T> callbacks) {
        Fragment parent = frag.getParentFragment();

        if (parent != null && callbacks.isInstance(parent)) {
            return (T) parent;

        } else {
            FragmentActivity activity = frag.getActivity();
            if (activity != null && callbacks.isInstance(activity)) {
                return (T) activity;
            }
        }

        // We haven't actually failed a class cast thanks to the checks above, but that's the
        // idiomatic approach for this pattern with fragments.
        throw new ClassCastException("This fragment's activity or parent fragment must implement "
                + callbacks.getCanonicalName());
    }

    public static Uri buildRawResourceUri(Context ctx, String filename) {
        // strip off any file extension from the video, because Android.
        return Uri.parse(
                String.format("android.resource://%s/raw/%s",
                        ctx.getPackageName(),
                        removeExtension(filename)));
    }

    /**
     * For when you don't care if the Closeable you're closing is null, and
     * you don't care that closing a buffer threw an exception, but
     * you still care enough that logging might be useful, like in a
     * "finally" block, after you've already returned to the caller.
     */
    public static void closeThisThingOrMaybeDont(@Nullable Closeable closeable) {
        if (closeable == null) {
            log.d("Can't close closable, arg was null.");
            return;
        }

        try {
            closeable.close();
        } catch (IOException e) {
            log.d("Couldn't close closable, but that's apparently OK.  Error was: " + e.getMessage());
        }
    }

    @Nullable
    private static String removeExtension(@Nullable String filename) {
        if (filename == null) {
            return null;
        }
        int indexOfExtension = filename.lastIndexOf(".");
        if (indexOfExtension == -1) {
            return filename;
        } else {
            return filename.substring(0, indexOfExtension);
        }
    }

}
