package io.particle.cloudsdk.example_app;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.io.IOException;

import io.particle.android.sdk.cloud.SparkCloud;
import io.particle.android.sdk.cloud.SparkCloudException;
import io.particle.android.sdk.cloud.SparkDevice;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.Toaster;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        findViewById(R.id.login_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        final String email = ((EditText) findViewById(R.id.email)).getText().toString();
                        final String password = ((EditText) findViewById(R.id.password)).getText().toString();

                        // Don't do:
                        AsyncTask task = new AsyncTask() {
                            @Override
                            protected Object doInBackground(Object[] params) {
                                try {
                                    SparkCloud.get(LoginActivity.this).logIn(email, password);
                                }
                                catch (final SparkCloudException e)
                                {
                                    Runnable mainThread = new Runnable() {
                                        @Override
                                        public void run() {
                                            Toaster.l(LoginActivity.this, e.getBestMessage());
                                            e.printStackTrace();
                                            Log.d("info", e.getBestMessage());
//                                            Log.d("info", e.getCause().toString());
                                        }
                                    };
                                    runOnUiThread(mainThread);

                                }

                                return null;
                            }

                        };
//                        task.execute();

                        //-------

                        // Do:
                        Async.executeAsync(SparkCloud.get(v.getContext()), new Async.ApiWork<SparkCloud, Integer>() {
                            private SparkDevice mDevice;
                            @Override
                            public Integer callApi(SparkCloud sparkCloud) throws SparkCloudException, IOException {
                                sparkCloud.logIn(email,password);
                                sparkCloud.getDevices();
                                mDevice = sparkCloud.getDevice("1f0034000747343232361234");
                                Integer variable;
                                try {
                                    variable = mDevice.getVariable("analogvalue");
                                }
                                catch (SparkDevice.VariableDoesNotExistException e)
                                {
                                    Toaster.s(LoginActivity.this,"Error reading variable");
                                    variable = -1;
                                }
                                return variable;

                            }

                            @Override
                            public void onSuccess(Integer value) {
                                Toaster.l(LoginActivity.this,"Logged in");
                                Intent intent = ValueActivity.buildIntent(LoginActivity.this, value, mDevice.getID());
                                startActivity(intent);
                            }

                            @Override
                            public void onFailure(SparkCloudException e) {
                                Toaster.l(LoginActivity.this, e.getBestMessage());
                                e.printStackTrace();
                                Log.d("info", e.getBestMessage());
                            }
                        });


                    }
                }

        );
    }


}
