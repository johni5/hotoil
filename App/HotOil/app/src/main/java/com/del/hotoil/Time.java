package com.del.hotoil;

import static java.lang.String.format;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Time {

    private int hours, minutes;

    public Time(int hours, int minutes) {
        this.hours = hours;
        this.minutes = minutes;
    }

    public int getHours() {
        return hours;
    }

    public void setHours(int hours) {
        this.hours = hours;
    }

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    public String formatToSend() {
        return _XX(hours) + _XX(minutes);
    }

    public String formatToShow() {
        return _XX(hours) + ":" + _XX(minutes);
    }

    private static final SimpleDateFormat fmt = new SimpleDateFormat("HHmm", Locale.forLanguageTag("RU"));

    public static Time current() {
        return parse(new Date());
    }

    public static Time parse(Date date) {
        return parse(fmt.format(date));
    }

    public static Time parse(String str) {
        if (str != null && str.trim().length() > 3) {
            return new Time(parseInt(str, 0), parseInt(str, 1));
        }
        throw new IllegalArgumentException(format("Bad time format '%s', 'hhmm' expected", str));
    }

    static int parseInt(String str, int pos) {
        // 0-2 | 2-4 | 4-6
        return Integer.parseInt(str.substring(pos * 2, Math.min(pos * 2 + 2, str.length())));
    }

    static String _XX(int v) {
        String s = "0" + v;
        return s.substring(s.length() - 2);
    }

}
