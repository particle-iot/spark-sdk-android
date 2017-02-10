package io.particle.cloudsdk.example_app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.IOException;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.Toaster;

public class ValueActivity extends AppCompatActivity {

    private static final String ARG_VALUE = "ARG_VALUE";
    private static final String ARG_DEVICEID = "ARG_DEVICEID";

    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_value);
        tv = (TextView) findViewById(R.id.value);
        tv.setText(String.valueOf(getIntent().getIntExtra(ARG_VALUE, 0)));

        findViewById(R.id.refresh_button).setOnClickListener(v -> {
            //...
            // Do network work on background thread
            Async.executeAsync(ParticleCloud.get(ValueActivity.this), new Async.ApiWork<ParticleCloud, Object>() {
                @Override
                public Object callApi(@NonNull ParticleCloud ParticleCloud) throws ParticleCloudException, IOException {
                    ParticleDevice device = ParticleCloud.getDevice(getIntent().getStringExtra(ARG_DEVICEID));
                    Object variable;
                    try {
                        variable = device.getVariable("analogvalue");
                    } catch (ParticleDevice.VariableDoesNotExistException e) {
                        Toaster.l(ValueActivity.this, e.getMessage());
                        variable = -1;
                    }
                    return variable;
                }

                @Override
                public void onSuccess(@NonNull Object i) { // this goes on the main thread
                    tv.setText(i.toString());
                }

                @Override
                public void onFailure(@NonNull ParticleCloudException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        new MenuInflater(this).inflate(R.menu.menu_value, menu);
        return (super.onCreateOptionsMenu(menu));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_info) {
            Intent intent = DeviceInfoActivity.buildIntent(ValueActivity.this, getIntent().getStringExtra(ARG_DEVICEID));
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }


    public static Intent buildIntent(Context ctx, Integer value, String deviceId) {
        Intent intent = new Intent(ctx, ValueActivity.class);
        intent.putExtra(ARG_VALUE, value);
        intent.putExtra(ARG_DEVICEID, deviceId);

        return intent;
    }


}
