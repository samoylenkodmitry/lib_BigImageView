package com.samart.bigimageview;

import android.util.Log;

public class Utils {
    private static final String ERROR_LOG = "oops";

    public static void log(final String mes) {
        Log.e(ERROR_LOG, Thread.currentThread().getName() + " " + mes);
    }

}
