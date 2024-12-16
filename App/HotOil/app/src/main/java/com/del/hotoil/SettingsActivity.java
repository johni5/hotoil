package com.del.hotoil;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.NumberPicker;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    public static final String SETTINGS_INTENT = "settings";

    private NumberPicker setTemperatureMax, setHeaterMin, setBatteryMin;
    private CheckBox setBattery, setHeater, setTemperature, setShakeOn;
    private Button btnSetExit;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setTemperatureMax = findViewById(R.id.setTemperatureMax);
        setHeaterMin = findViewById(R.id.setHeaterMin);
        setBatteryMin = findViewById(R.id.setBatteryMin);
        setBattery = findViewById(R.id.setBattery);
        setHeater = findViewById(R.id.setHeater);
        setTemperature = findViewById(R.id.setTemperature);
        setShakeOn = findViewById(R.id.setShakeOn);
        btnSetExit = findViewById(R.id.btnSetExit);

        setTemperatureMax.setMinValue(0);
        setTemperatureMax.setMaxValue(5);
        setHeaterMin.setMinValue(1);
        setHeaterMin.setMaxValue(3);
        setBatteryMin.setMinValue(0);
        setBatteryMin.setMaxValue(15);

        btnSetExit.setOnClickListener(e -> {
            Settings s = new Settings();
            s.setShakeOn(setShakeOn.isChecked());
            s.setBatteryOn(setBattery.isChecked());
            s.setTemperatureOn(setTemperature.isChecked());
            s.setHeaterOn(setHeater.isChecked());
            s.setTemperatureGrad(setTemperatureMax.getValue());
            s.setHeaterAmperage(setHeaterMin.getValue());
            s.setBatteryVolts(setBatteryMin.getValue());

            Intent resultIntent = new Intent();
            resultIntent.putExtra(SETTINGS_INTENT, s);
            setResult(RESULT_OK, resultIntent);
            finish();
        });


    }
}
