package com.tuya.api.client;


import com.google.gson.Gson;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.tuya.api.client.token.TokenClient;
import com.tuya.api.common.*;
import com.tuya.api.exception.TuyaCloudSDKException;
import com.tuya.api.utils.GsonUtil;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import java.net.URL;
import java.util.Map;

/**
 * 通用客户端类
 */
public class CommonClient {

    /**
     * 重试次数
     */
    private static final int maxRetry = 3;

    /**
     * 执行请求
     */
    public static <T> TuyaResult<T> sendRequest(String url, HttpMethod method, Map<String, String> header, Object body) {
        TuyaResult<T> result = null;
        int retry = CommonClient.maxRetry;
        boolean retryFlag = Boolean.TRUE;

        while (retry >= 0 && retryFlag) {
            try {
                result = execute(url, method, header, body);
                retryFlag = Boolean.FALSE;
            } catch (TuyaCloudSDKException e) {
                // token无效，重新获取token
                if (e.getCode() != null && 1010 == e.getCode() && retry > 0) {
                    retry--;
                    TokenClient.getToken();
                } else {
                    throw e;
                }
            }
        }

        return result;
    }


    /**
     * 执行请求
     */
    private static <T> TuyaResult<T> execute(String url, HttpMethod method, Map<String, String> header, Object body) {
        try {
            // 验证开发者信息
            if (MapUtils.isEmpty(Constant.map)) {
                throw new TuyaCloudSDKException("未初始化开发者信息！");
            }

            if (StringUtils.isNotBlank(url) && !url.startsWith("http")) {
                url = Constant.map.get(Constant.ENDPOINT) + url;
            }
            String bodyStr = "";
            if (body != null) {
                bodyStr = new Gson().toJson(body);
            }
            Request.Builder request;
            if (HttpMethod.GET.equals(method)) {
                request = RequestHandler.getRequest(url);
            } else if (HttpMethod.POST.equals(method)) {
                request = RequestHandler.postRequest(url, bodyStr);
            } else if (HttpMethod.PUT.equals(method)) {
                request = RequestHandler.putRequest(url, bodyStr);
            } else if (HttpMethod.DELETE.equals(method)) {
                request = RequestHandler.deleteRequest(url, bodyStr);
            } else {
                throw new TuyaCloudSDKException("Method only support GET, POST, PUT, DELETE");
            }
            Headers headers = RequestHandler.getHeader(true, request.build(), bodyStr, header);
            request.headers(headers);
            request.url(Constant.map.get(Constant.ENDPOINT) + RequestHandler.getPathAndSortParam(new URL(url)));
            Response response = RequestHandler.doRequest(request.build());

            TuyaResult result = GsonUtil.gson().fromJson(response.body().string(), TuyaResult.class);
            if (!result.getSuccess()) {
                throw new TuyaCloudSDKException(result.getCode(), ErrorCode.map.get(result.getCode()));
            }
            return result;
        } catch (Exception e) {
            throw new TuyaCloudSDKException(e.getMessage());
        }
    }
}
