package com.del.hotoil;

import java.nio.charset.StandardCharsets;

public enum Cmd {

    MODE("MODE"),
    STATE("STATE"),
    STATE_DESC("STATEM"),
    TIME("TI"),
    TIME_ON(""),
    BATTERY_VOLT("VB"),
    BATTERY_VOLT_MIN("VBMIN"),
    TEMPERATURE_GRAD("TG"),
    TEMPERATURE_GRAD_MAX("TGMAX"),
    SHAKE_ACC("SHACC"),
    SHAKE_GYR("SHGYR"),
    HEATER_AMPERAGE("HTI"),
    CONF("CONF"),
    CONF_DESC("CONFM");

    private String code;

    Cmd(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public byte[] getBytes(String params) {
        StringBuffer s = new StringBuffer(code);
        if (params != null && !params.trim().isEmpty()) {
            s.append(" ").append(params);
        }
        return s.toString().getBytes(StandardCharsets.UTF_8);
    }
}
