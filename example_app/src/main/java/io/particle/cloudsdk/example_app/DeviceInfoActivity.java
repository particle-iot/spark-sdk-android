package io.particle.cloudsdk.example_app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.io.IOException;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.utils.Async;

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
        nameView = (TextView) findViewById(R.id.name);
        productIdView = (TextView) findViewById(R.id.productId);
        platformIdView = (TextView) findViewById(R.id.platformId);
        ipAddressView = (TextView) findViewById(R.id.ipAddress);
        lastAppNameView = (TextView) findViewById(R.id.lastAppName);
        statusView = (TextView) findViewById(R.id.status);
        lastHeardView = (TextView) findViewById(R.id.lastHeard);
        cellularView = (TextView) findViewById(R.id.cellular);
        imeiView = (TextView) findViewById(R.id.imei);
        currentBuildView = (TextView) findViewById(R.id.currentBuild);
        defaultBuildView = (TextView) findViewById(R.id.defaultBuild);

        Async.executeAsync(ParticleCloud.get(this), new Async.ApiWork<ParticleCloud, ParticleDevice>() {
            @Override
            public ParticleDevice callApi(@NonNull ParticleCloud ParticleCloud) throws ParticleCloudException, IOException {
                return ParticleCloud.getDevice(getIntent().getStringExtra(ARG_DEVICEID));
            }

            @Override
            public void onSuccess(@NonNull ParticleDevice particleDevice) { // this goes on the main thread
                nameView.setText("Name: " + particleDevice.getName());
                productIdView.setText("Product id: " + particleDevice.getProductID());
                platformIdView.setText("Platform id: " + particleDevice.getPlatformID());
                ipAddressView.setText("Ip address: " + particleDevice.getIpAddress());
                lastAppNameView.setText("Last app name: " + particleDevice.getLastAppName());
                statusView.setText("Status: " + particleDevice.getStatus());
                lastHeardView.setText("Last heard: " + particleDevice.getLastHeard());
                cellularView.setText("Cellular: " + particleDevice.isCellular());
                imeiView.setText("Imei: " + particleDevice.getImei());
                currentBuildView.setText("Current build: " + particleDevice.getCurrentBuild());
                defaultBuildView.setText("Default build: " + particleDevice.getDefaultBuild());
            }

            @Override
            public void onFailure(@NonNull ParticleCloudException e) {
                e.printStackTrace();
            }
        });
    }

    public static Intent buildIntent(Context ctx, String deviceId) {
        Intent intent = new Intent(ctx, DeviceInfoActivity.class);
        intent.putExtra(ARG_DEVICEID, deviceId);

        return intent;
    }
}
