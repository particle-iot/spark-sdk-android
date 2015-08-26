package io.particle.android.sdk.utils;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.widget.Toast;

public class Toaster {

    /**
     * Shows a short toast message, this will always executes on the main thread
     * @param activity
     * @param msg
     */

    public static void s(@NonNull final Activity activity, final String msg) {
        EZ.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
            }
        });

    }

    /**
     * Shows a long toast message, this will always executes on the main thread
     * @param activity
     * @param msg
     */
    public static void l(@NonNull final Activity activity, final String msg) {

        EZ.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
            }
        });

    }

}
