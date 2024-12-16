package com.del.hotoil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class Response {

    private String r;

    public Response(String r) {
        this.r = r;
    }

    public boolean isError() {
        return r == null || r.trim().equalsIgnoreCase("ERROR");
    }

    public Status getStatus() {
        return new Status(Integer.parseInt(Objects.requireNonNull(r.trim())));
    }

    public String formatFloat() {
        return new BigDecimal(Objects.requireNonNull(r.trim())).setScale(2, RoundingMode.HALF_EVEN).toString();
    }

    public String formatTime() {
        if (r != null && r.trim().length() > 3) {
            return r.trim().substring(0, 2) + ":" + r.trim().substring(2, 4);
        }
        return "-";
    }

    public int getTimeHH() {
        if (r != null && r.trim().length() > 1) {
            return Integer.parseInt(r.trim().substring(0, 2));
        }
        return 0;
    }

    public int getTimeMM() {
        if (r != null && r.trim().length() > 3) {
            return Integer.parseInt(r.trim().substring(2, 4));
        }
        return 0;
    }

    public int getTimeTimeout() {
        if (r != null && r.trim().length() > 5) {
            return Integer.parseInt(r.trim().substring(4, 6));
        }
        return 0;
    }

}
