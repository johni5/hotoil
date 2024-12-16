package com.del.hotoil;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.ui.AppBarConfiguration;

import com.del.hotoil.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements ConnectionListener {

    private static final String DEVICE_NAME = "HOTOIL-01";
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private NumberPicker timeoutNumber;
    private TextView temperatureTxt, batteryTxt, heaterTxt, timerInfoTxt, deviceInfo, localTime;
    private TimePicker timePicker;
    private ImageButton powerBtn;
    private ImageView connectionIV, waveIV, temperatureIV, accumulatorIV, heaterIV, clockIV;
    private ConnectionManager connectionManager;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<Intent> settingsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        Settings s = (Settings) data.getSerializableExtra(SettingsActivity.SETTINGS_INTENT);
                        if (s != null) {
                            Log.d(Utils.TAG, s.toString());
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        timeoutNumber = findViewById(R.id.timeoutNumber);
        temperatureTxt = findViewById(R.id.temperatureTxt);
        batteryTxt = findViewById(R.id.batteryTxt);
        heaterTxt = findViewById(R.id.heaterTxt);
        timerInfoTxt = findViewById(R.id.timerInfoTxt);
        deviceInfo = findViewById(R.id.deviceInfo);
        localTime = findViewById(R.id.localTime);
        powerBtn = findViewById(R.id.powerBtn);
        timePicker = findViewById(R.id.timePicker);
        connectionIV = findViewById(R.id.connectionIV);
        waveIV = findViewById(R.id.waveIV);
        temperatureIV = findViewById(R.id.temperatureIV);
        accumulatorIV = findViewById(R.id.accumulatorIV);
        heaterIV = findViewById(R.id.heaterIV);
        clockIV = findViewById(R.id.clockIV);

        timePicker.setIs24HourView(true);
        timeoutNumber.setMinValue(1);
        timeoutNumber.setMaxValue(99);


        connectionIV.setOnClickListener(e -> {
            if (connectionManager.isConnected()) {
                connectionManager.close();
            } else {
                deviceInfo.setText(R.string.device_find);
                connectionManager.begin(DEVICE_NAME);
            }
        });

//        sendBtn.setOnClickListener(e -> {
//            CharSequence text = cmdText.getText();
//            if (text != null && text.length() > 0) {
//                connectionManager.sendAndWaitResponse(text.toString(), r -> {
//                    handler.post(() -> {
//                        if (r != null) outText.setText(r);
//                    });
//                });
//                cmdText.setText("");
//            }
//        });

        connectionManager = new ConnectionManager(this);
        connectionManager.setListener(this);
        updateStatus();
    }

    @Override
    protected void onStart() {
        super.onStart();
        deviceInfo.setText(R.string.device_find);
        if (!connectionManager.begin(DEVICE_NAME)) {
            Toast.makeText(this, R.string.no_bluetooth, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connectionManager.close();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        connectionManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void updateStatus() {
        if (connectionManager.isConnected()) {
            connectionIV.setBackgroundTintList(getColorStateList(R.color.Connection_ON));
            deviceInfo.setText(connectionManager.getDeviceInfo());
            deviceInfo.setTextColor(getColorStateList(R.color.Connection_ON));
            connectionManager.sendAndWaitResponse(Cmd.STATE, r1 -> {
                Status status = r1.getStatus();
                Mode mode = status.getMode();
                runOnUiThread(() -> {
                    powerBtn.setBackgroundTintList(getColorStateList(status.isOn() ? R.color.Power_ON : R.color.Power_OFF));
                    if (mode.equals(Mode.IDLE)) {
                        timerInfoTxt.setText(R.string.mode_idle_info);
                    }
                    waveIV.setBackgroundTintList(getColorStateList(status.isShakeDetected() ? R.color.ImageView_BAD : R.color.ImageView_ON));
                    temperatureIV.setBackgroundTintList(getColorStateList(status.isHiTemperature() ? R.color.ImageView_BAD : R.color.ImageView_ON));
                    accumulatorIV.setBackgroundTintList(getColorStateList(status.isLowBattery() ? R.color.ImageView_BAD : R.color.ImageView_ON));
                    heaterIV.setBackgroundTintList(getColorStateList(status.isBadHeater() ? R.color.ImageView_BAD : R.color.ImageView_ON));
                    clockIV.setBackgroundTintList(getColorStateList(status.isBadClock() ? R.color.ImageView_BAD : R.color.ImageView_ON));
                });
                connectionManager.sendAndWaitResponse(Cmd.TEMPERATURE_GRAD, r2 -> {
                    temperatureTxt.setText(r2.formatFloat());
                }, ex2 -> {
                    Utils.error(ex2);
                    runOnUiThread(() -> {
                        temperatureTxt.setText("-");
                        Toast.makeText(this, R.string.response_error, Toast.LENGTH_LONG).show();
                    });
                });
                connectionManager.sendAndWaitResponse(Cmd.BATTERY_VOLT, r2 -> {
                    batteryTxt.setText(r2.formatFloat());
                }, ex2 -> {
                    Utils.error(ex2);
                    runOnUiThread(() -> {
                        batteryTxt.setText("-");
                        Toast.makeText(this, R.string.response_error, Toast.LENGTH_LONG).show();
                    });
                });
                connectionManager.sendAndWaitResponse(Cmd.HEATER_AMPERAGE, r2 -> {
                    heaterTxt.setText(r2.formatFloat());
                }, ex2 -> {
                    Utils.error(ex2);
                    runOnUiThread(() -> {
                        heaterTxt.setText("-");
                        Toast.makeText(this, R.string.response_error, Toast.LENGTH_LONG).show();
                    });
                });
                connectionManager.sendAndWaitResponse(Cmd.TIME, r2 -> {
                    localTime.setText(r2.formatTime());
                }, ex2 -> {
                    Utils.error(ex2);
                    runOnUiThread(() -> {
                        Toast.makeText(this, R.string.response_error, Toast.LENGTH_LONG).show();
                    });
                });

                connectionManager.sendAndWaitResponse(Cmd.TIME_ON, r2 -> {
                    timePicker.setHour(r2.getTimeHH());
                    timePicker.setMinute(r2.getTimeMM());
                    timeoutNumber.setValue(r2.getTimeTimeout());
                }, ex2 -> {
                    Utils.error(ex2);
                    runOnUiThread(() -> {
                        Toast.makeText(this, R.string.response_error, Toast.LENGTH_LONG).show();
                    });
                });

            }, ex1 -> {
                Utils.error(ex1);
                runOnUiThread(() -> Toast.makeText(this, R.string.response_error, Toast.LENGTH_LONG).show());
            });
        } else {
            connectionIV.setBackgroundTintList(getColorStateList(R.color.Connection_OFF));
            deviceInfo.setText(R.string.device_info);
            deviceInfo.setTextColor(getColorStateList(R.color.Connection_OFF));
            temperatureTxt.setText("-");
            batteryTxt.setText("-");
            heaterTxt.setText("-");
            timerInfoTxt.setText("-");
            localTime.setText("-");
            powerBtn.setBackgroundTintList(getColorStateList(R.color.Power_OFF));
            waveIV.setBackgroundTintList(getColorStateList(R.color.ImageView_OFF));
            temperatureIV.setBackgroundTintList(getColorStateList(R.color.ImageView_OFF));
            accumulatorIV.setBackgroundTintList(getColorStateList(R.color.ImageView_OFF));
            heaterIV.setBackgroundTintList(getColorStateList(R.color.ImageView_OFF));
            clockIV.setBackgroundTintList(getColorStateList(R.color.ImageView_OFF));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            settingsLauncher.launch(intent);
            return true;
        }
        if (id == R.id.action_exit) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnect(String deviceName) {
        updateStatus();
//        String stInfo = device.getName();
//        deviceText.setText(stInfo);
//        connectionText.setText("Подключено");
    }

    @Override
    public void onDisconnect() {
        updateStatus();
//        deviceText.setText("Устройство");
//        connectionText.setText("Отключено");
    }
}