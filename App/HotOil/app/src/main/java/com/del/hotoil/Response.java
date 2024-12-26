package com.del.hotoil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class Response {

    private final String r;
    private final Cmd cmd;
    private Exception exception;

    public Response(Cmd cmd, String r) {
        this.r = r;
        this.cmd = cmd;
    }

    public Cmd getCmd() {
        return cmd;
    }

    public boolean isError() {
        return r == null || r.trim().equalsIgnoreCase("ERROR");
    }

    public Status getStatus() {
        return new Status(Integer.parseInt(Objects.requireNonNull(r)));
    }

    public String formatFloat() {
        return new BigDecimal(Objects.requireNonNull(r)).setScale(2, RoundingMode.HALF_EVEN).toString();
    }

    public String formatTime() {
        if (r != null && r.trim().length() > 3) {
            return r.trim().substring(0, 2) + ":" + r.trim().substring(2, 4);
        }
        return "-";
    }

    public Time getTime() {
        return Time.parse(Objects.requireNonNull(r));
    }

    public Timer getTimer() {
        return Timer.parse(Objects.requireNonNull(r));
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }
}
