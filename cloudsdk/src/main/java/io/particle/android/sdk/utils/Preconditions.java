package io.particle.android.sdk.utils;

/**
 * Like Guava's Preconditions, but without the overwhelming method count cost
 */
public class Preconditions {

    public static void checkArgument(boolean condition, String errorMessage) {
        if (!condition) {
            throw new IllegalArgumentException(String.valueOf(errorMessage));
        }
    }

}
