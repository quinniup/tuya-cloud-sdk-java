package com.tuya.api.common;

public interface ApiRequest {

    /**
     * 请求方法
     */
    HttpMethod getRequestMethod();

    /**
     * 请求url
     */
    String getRequestUrl();
}
