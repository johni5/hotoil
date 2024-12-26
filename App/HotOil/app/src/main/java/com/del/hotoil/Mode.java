package com.del.hotoil;

public enum Mode {

    IDLE(0),
    WAIT(1),
    CALIBRATION(2),
    MANUAL(3);

    private int code;

    Mode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static Mode parse(int code) {
        for (Mode mode : values()) {
            if (mode.equals(mode.code)) return mode;
        }
        throw new IllegalArgumentException("code " + code + " not support");
    }

}
