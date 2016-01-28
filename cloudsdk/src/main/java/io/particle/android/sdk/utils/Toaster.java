package io.particle.android.sdk.utils;

import android.app.Activity;
import android.support.annotation.Nullable;
import android.widget.Toast;

import javax.annotation.ParametersAreNonnullByDefault;


@ParametersAreNonnullByDefault
public class Toaster {

    /**
     * Shows a toast message for a short time.
     * <p/>
     * This is safe to call from background/worker threads.
     */
    public static void s(final Activity activity, @Nullable final String msg) {
        showToast(activity, msg, Toast.LENGTH_SHORT);
    }

    /**
     * Shows a toast message for a longer time than {@link #s(Activity, String)}.
     * <p/>
     * This is safe to call from background/worker threads.
     */
    public static void l(final Activity activity, @Nullable final String msg) {
        showToast(activity, msg, Toast.LENGTH_LONG);
    }


    private static void showToast(final Activity activity, @Nullable final String msg,
                                  final int length) {
        Runnable toastRunnable = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, msg, length).show();
            }
        };

        if (EZ.isThisTheMainThread()) {
            toastRunnable.run();
        } else {
            EZ.runOnMainThread(toastRunnable);
        }
    }
}
