package com.del.hotoil;

import java.io.Serializable;

public class Settings implements Serializable {

    private boolean shakeOn;
    private boolean temperatureOn;
    private boolean heaterOn;
    private boolean batteryOn;

    private int temperatureGrad;
    private int heaterAmperage;
    private int batteryVolts;

    public boolean isShakeOn() {
        return shakeOn;
    }

    public void setShakeOn(boolean shakeOn) {
        this.shakeOn = shakeOn;
    }

    public boolean isTemperatureOn() {
        return temperatureOn;
    }

    public void setTemperatureOn(boolean temperatureOn) {
        this.temperatureOn = temperatureOn;
    }

    public boolean isHeaterOn() {
        return heaterOn;
    }

    public void setHeaterOn(boolean heaterOn) {
        this.heaterOn = heaterOn;
    }

    public boolean isBatteryOn() {
        return batteryOn;
    }

    public void setBatteryOn(boolean batteryOn) {
        this.batteryOn = batteryOn;
    }

    public int getTemperatureGrad() {
        return temperatureGrad;
    }

    public void setTemperatureGrad(int temperatureGrad) {
        this.temperatureGrad = temperatureGrad;
    }

    public int getHeaterAmperage() {
        return heaterAmperage;
    }

    public void setHeaterAmperage(int heaterAmperage) {
        this.heaterAmperage = heaterAmperage;
    }

    public int getBatteryVolts() {
        return batteryVolts;
    }

    public void setBatteryVolts(int batteryVolts) {
        this.batteryVolts = batteryVolts;
    }

    @Override
    public String toString() {
        return "Settings{" +
                "shakeOn=" + shakeOn +
                ", temperatureOn=" + temperatureOn +
                ", heaterOn=" + heaterOn +
                ", batteryOn=" + batteryOn +
                ", temperatureGrad=" + temperatureGrad +
                ", heaterAmperage=" + heaterAmperage +
                ", batteryVolts=" + batteryVolts +
                '}';
    }
}
