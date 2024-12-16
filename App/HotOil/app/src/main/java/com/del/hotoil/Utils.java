package com.del.hotoil;

import android.util.Log;

import java.io.Closeable;
import java.io.IOException;

public class Utils {

    public static final String TAG = "HOTOIL_APP";

    public static void error(String m, Exception e) {
        Log.e(TAG, m, e);
    }

    public static void error(Exception e) {
        error(e.getMessage(), e);
    }

    public static void close(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception e) {
                error("Could not close the client socket", e);
            }
        }
    }

    public static String nvl(String v1, String v2) {
        return v1 == null ? v2 : v1;
    }

}
