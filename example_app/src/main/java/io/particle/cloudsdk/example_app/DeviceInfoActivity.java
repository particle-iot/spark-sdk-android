package io.particle.cloudsdk.example_app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.Locale;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.cloud.models.DeviceStateChange;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.Toaster;

/**
 * Created by Julius.
 */

public class DeviceInfoActivity extends AppCompatActivity {

    private static final String ARG_DEVICEID = "ARG_DEVICEID";

    private TextView nameView, platformIdView, productIdView, ipAddressView, lastAppNameView,
            cellularView, imeiView, currentBuildView, defaultBuildView, statusView, lastHeardView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);
        //Get and display device information
        nameView = findViewById(R.id.name);
        productIdView = findViewById(R.id.productId);
        platformIdView = findViewById(R.id.platformId);
        ipAddressView = findViewById(R.id.ipAddress);
        lastAppNameView = findViewById(R.id.lastAppName);
        statusView = findViewById(R.id.status);
        lastHeardView = findViewById(R.id.lastHeard);
        cellularView = findViewById(R.id.cellular);
        imeiView = findViewById(R.id.imei);
        currentBuildView = findViewById(R.id.currentBuild);
        defaultBuildView = findViewById(R.id.defaultBuild);

        Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, ParticleDevice>() {
            @Override
            public ParticleDevice callApi(@NonNull ParticleCloud ParticleCloud) throws ParticleCloudException, IOException {
                ParticleDevice device = ParticleCloud.getDevice(getIntent().getStringExtra(ARG_DEVICEID));
                device.subscribeToSystemEvents();
                return device;
            }

            @Override
            public void onSuccess(@NonNull ParticleDevice particleDevice) { // this goes on the main thread
                nameView.setText(String.format("Name: %s", particleDevice.getName()));
                productIdView.setText(String.format(Locale.getDefault(), "Product id: %d", particleDevice.getProductID()));
                platformIdView.setText(String.format(Locale.getDefault(), "Platform id: %d", particleDevice.getPlatformID()));
                ipAddressView.setText(String.format("Ip address: %s", particleDevice.getIpAddress()));
                lastAppNameView.setText(String.format("Last app name: %s", particleDevice.getLastAppName()));
                statusView.setText(String.format("Status: %s", particleDevice.getStatus()));
                lastHeardView.setText(String.format("Last heard: %s", particleDevice.getLastHeard()));
                cellularView.setText(String.format("Cellular: %s", particleDevice.isCellular()));
                imeiView.setText(String.format("Imei: %s", particleDevice.getImei()));
                currentBuildView.setText(String.format("Current build: %s", particleDevice.getCurrentBuild()));
                defaultBuildView.setText(String.format("Default build: %s", particleDevice.getDefaultBuild()));
            }

            @Override
            public void onFailure(@NonNull ParticleCloudException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        //Register to EventBus for system event listening
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onEvent(DeviceStateChange deviceStateChange) {
        Toaster.l(this, deviceStateChange.getDevice().getName() + " system event received");
        //unsubscribe from further system events
        try {
            deviceStateChange.getDevice().unsubscribeFromSystemEvents();
        } catch (ParticleCloudException e) {
            Toaster.l(this, "Failed to unsubscribe.");
        }
    }

    public static Intent buildIntent(Context ctx, String deviceId) {
        Intent intent = new Intent(ctx, DeviceInfoActivity.class);
        intent.putExtra(ARG_DEVICEID, deviceId);

        return intent;
    }
}
