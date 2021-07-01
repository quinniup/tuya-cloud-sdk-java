package com.tuya.api.utils;

import com.google.gson.Gson;

/**
 * @author gongtai.yin
 * @since 2021/07/02
 */
public class GsonUtil {

    private final Gson gson = new Gson().newBuilder().create();

    public static Gson gson() {
        return new GsonUtil().gson;
    }

    public static Gson gson(boolean isNullValue) {
        GsonUtil g = new GsonUtil();
        return g.gson.newBuilder().serializeNulls().create();
    }
}
