package io.particle.android.sdk.cloud;

import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import io.particle.android.sdk.cloud.ApiDefs.CloudApi;
import io.particle.android.sdk.cloud.Responses.Models;
import io.particle.android.sdk.cloud.Responses.Models.CompleteDevice;
import io.particle.android.sdk.cloud.Responses.Models.SimpleDevice;

import static io.particle.android.sdk.utils.Py.list;

/**
 * Does parallel fetching of {@link Models.CompleteDevice}
 *
 * FIXME: review this solution
 */
@ParametersAreNonnullByDefault
class ParallelDeviceFetcher {

    static class DeviceFetchResult {

        final String deviceId;
        /**
         * Will be null if device could not be fetched.
         */
        @Nullable
        final CompleteDevice fetchedDevice;

        DeviceFetchResult(String deviceId, @Nullable CompleteDevice fetchedDevice) {
            this.deviceId = deviceId;
            this.fetchedDevice = fetchedDevice;
        }
    }


    // FIXME: insert slower API here
    static ParallelDeviceFetcher newFetcherUsingExecutor(ExecutorService executor) {
        return new ParallelDeviceFetcher(executor);
    }


    private final ExecutorService executor;

    private ParallelDeviceFetcher(ExecutorService executor) {
        this.executor = executor;
    }

    // FIXME: ugh, so lame.  Figure out the smarter way to do per-device fetch timeouts
    // without having to resort to two Retrofit API instances.  look into jsr166 ForkJoinPool
    // or similar (since we can't use the Java 7 one until API 21...)
    /**
     * Fetch the devices in parallel.  Ordering of results not guaranteed to be preserved or
     * respected in any way.
     */
    @CheckResult
    Collection<DeviceFetchResult> fetchDevicesInParallel(Collection<SimpleDevice> simpleDevices,
                                                         final CloudApi cloudApi,
                                                         int perDeviceTimeoutInSeconds) {
        // Assemble the list of Callables
        List<Callable<DeviceFetchResult>> callables = list();
        for (final SimpleDevice device : simpleDevices) {
            callables.add(new Callable<DeviceFetchResult>() {
                @Override
                public DeviceFetchResult call() throws Exception {
                    return getDevice(cloudApi, device.id);
                }
            });
        }


        // Submit callables, receive list of Futures.  invokeAll() will block until they finish.
        List<Future<DeviceFetchResult>> futures = list();
        try {
            long timeout = perDeviceTimeoutInSeconds * simpleDevices.size();
            futures = executor.invokeAll(callables, timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // FIXME: think about what to do in this implausible(?) scenario, or how to avoid it.
            e.printStackTrace();
        }


        // turn the results into something usable.
        List<DeviceFetchResult> results = list();
        for (Future<DeviceFetchResult> future : futures) {
            try {
                DeviceFetchResult result = future.get();
                if (result != null) {
                    results.add(result);
                }
            } catch (InterruptedException | ExecutionException e) {
                // FIXME: see above; think more about what to do in this scenario, or how to avoid it.
                e.printStackTrace();
            }
        }

        return results;
    }


    private DeviceFetchResult getDevice(CloudApi cloudApi, String deviceID) {
        CompleteDevice device = null;
        try {
            device = cloudApi.getDevice(deviceID);
        } catch (Exception e) {
            // doesn't matter why it fails, just don't abort the whole operation because of it
        }
        return new DeviceFetchResult(deviceID, device);
    }


}
