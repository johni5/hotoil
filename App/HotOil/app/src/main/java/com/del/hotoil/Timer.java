package com.del.hotoil;

import static java.lang.String.format;

public class Timer extends Time {

    private int delayMinutes;
    private int delayOnSeconds;

    public Timer(int hours, int minutes, int delayMinutes, int delayOnSeconds) {
        super(hours, minutes);
        this.delayMinutes = delayMinutes;
        this.delayOnSeconds = delayOnSeconds;
    }

    public int getDelayMinutes() {
        return delayMinutes;
    }

    public void setDelayMinutes(int delayMinutes) {
        this.delayMinutes = delayMinutes;
    }

    public int getDelayOnSeconds() {
        return delayOnSeconds;
    }

    public void setDelayOnSeconds(int delayOnSeconds) {
        this.delayOnSeconds = delayOnSeconds;
    }

    public String formatToSend() {
        return super.formatToSend() + _XX(delayMinutes);
    }

    public static Timer parse(String str) {
        if (str != null && str.trim().length() > 6) {
            return new Timer(parseInt(str, 0), parseInt(str, 1), parseInt(str, 2), parseInt(str, 3));
        }
        throw new IllegalArgumentException(format("Bad time format '%s', 'hhmmxxy' expected", str));
    }

}
