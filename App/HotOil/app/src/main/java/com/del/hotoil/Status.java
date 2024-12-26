package com.del.hotoil;

public class Status {

    private int status;

    public Status(int status) {
        this.status = status;
    }

    public boolean isOn() {
        return (status & 1) > 0;
    }

    public Mode getMode() {
        return Mode.parse(((status >> 1) & 0b11));
    }

    public boolean isLowBattery() {
        return (status & 0b1000) > 0;
    }

    public boolean isHiTemperature() {
        return (status & 0b10000) > 0;
    }

    public boolean isShakeDetected() {
        return (status & 0b100000) > 0;
    }

    public boolean isBadHeater() {
        return (status & 0b1000000) > 0;
    }

    public boolean isBadClock() {
        return (status & 0b10000000) > 0;
    }

    @Override
    public String toString() {
        return "Status{\n" +
                "\tON=" + isOn() + "\n" +
                "\tMODE=" + getMode() + "\n" +
                "\tLow battery=" + isLowBattery() + "\n" +
                "\tHi temperature=" + isHiTemperature() + "\n" +
                "\tShake detected=" + isShakeDetected() + "\n" +
                "\tBad heater=" + isBadHeater() + "\n" +
                "\tBad clock=" + isBadClock() + "\n" +
                '}';
    }
}
