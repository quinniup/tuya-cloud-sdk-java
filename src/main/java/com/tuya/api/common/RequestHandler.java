package com.tuya.api.common;

import com.squareup.okhttp.*;
import com.tuya.api.client.token.TokenClient;
import com.tuya.api.exception.TuyaCloudSDKException;
import com.tuya.api.utils.GsonUtil;
import com.tuya.api.utils.Sha256Util;
import com.tuya.api.utils.ValidationUtil;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 请求处理类
 */
public class RequestHandler {

    private static final MediaType CONTENT_TYPE = MediaType.parse("application/json");
    private static final String EMPTY_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String SING_HEADER_NAME = "Signature-Headers";
    private static final String NONE_STRING = "";

    /**
     * 读超时时间（秒）
     */
    private static final int readTimeout = 30;

    /**
     * 写超时时间（秒）
     */
    private static final int writeTimeout = 30;

    /**
     * 连接超时时间（秒）
     */
    private static final int connTimeout = 30;

    /**
     * 重试次数
     */
    private static final int maxRetry = 3;


    /**
     * 执行请求, 默认需要携带token
     *
     * @param ar
     * @return
     * @throws Exception
     */
    public static TuyaResult sendRequest(ApiRequest ar) {
        return sendRequest(ar, Boolean.TRUE, new HashMap<>());
    }

    public static TuyaResult sendRequest(ApiRequest ar, Boolean isWithToken) {
        return sendRequest(ar, isWithToken, new HashMap<>());
    }

    public static TuyaResult sendRequest(ApiRequest ar, Map<String, String> customHeaders) {
        return sendRequest(ar, Boolean.TRUE, customHeaders);
    }


    /**
     * 执行请求, token过期自动重试
     *
     * @param ar
     * @param withToken
     * @return
     */
    public static TuyaResult sendRequest(ApiRequest ar, Boolean withToken, Map<String, String> customHeaders) {
        TuyaResult result = null;
        int retry = RequestHandler.maxRetry;
        boolean retryFlag = Boolean.TRUE;

        while (retry >= 0 && retryFlag) {
            try {
                result = execute(ar, withToken, customHeaders);
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
     *
     * @param ar        请求实体
     * @param withToken 是否需要token
     * @return
     */
    private static TuyaResult execute(ApiRequest ar, Boolean withToken, Map<String, String> customHeaders) {
        try {
            // 验证开发者信息
            if (MapUtils.isEmpty(Constant.map)) {
                throw new TuyaCloudSDKException("未初始化开发者信息！");
            }

            // 验证请求参数
            ValidationUtil.validateBean(ar);
            HttpMethod method = ar.getRequestMethod();
            String url = Constant.map.get(Constant.ENDPOINT) + ar.getRequestUrl();
            String body = "";
            if (ar instanceof ApiRequestBody) {
                body = ((ApiRequestBody) ar).getRequestBody();
            }
            Request.Builder request;
            if (HttpMethod.GET.equals(method)) {
                request = getRequest(url);
            } else if (HttpMethod.POST.equals(method)) {
                request = postRequest(url, body);
            } else if (HttpMethod.PUT.equals(method)) {
                request = putRequest(url, body);
            } else if (HttpMethod.DELETE.equals(method)) {
                request = deleteRequest(url, body);
            } else {
                throw new TuyaCloudSDKException("Method only support GET, POST, PUT, DELETE");
            }
            if (customHeaders.isEmpty()) {
                customHeaders = new HashMap<>();
            }
            Headers headers = getHeader(withToken, request.build(), body, customHeaders);
            request.headers(headers);
            request.url(Constant.map.get(Constant.ENDPOINT) + getPathAndSortParam(new URL(url)));
            Response response = doRequest(request.build());
            TuyaResult result = GsonUtil.gson().fromJson(response.body().string(), TuyaResult.class);
            if (!result.getSuccess()) {
                throw new TuyaCloudSDKException(result.getCode(), ErrorCode.map.get(result.getCode()));
            }
            return result;
        } catch (Exception e) {
            throw new TuyaCloudSDKException(e.getMessage());
        }
    }

    /**
     * 生成header
     *
     * @param withToken 是否需要携带token
     * @param headerMap 自定义header
     */
    public static Headers getHeader(Boolean withToken, Request request, String body, Map<String, String> headerMap) throws Exception {
        Headers.Builder hb = new Headers.Builder();

        Map<String, String> flattenHeaders = flattenHeaders(headerMap);
        String t = flattenHeaders.get("t");
        if (StringUtils.isBlank(t)) {
            t = System.currentTimeMillis() + "";
        }

        hb.add("client_id", Constant.map.get(Constant.ACCESS_ID));
        hb.add("t", t);
        hb.add("sign_method", "HMAC-SHA256");
        hb.add("lang", "zh");
        hb.add(SING_HEADER_NAME, flattenHeaders.getOrDefault(SING_HEADER_NAME, ""));
        String nonceStr = flattenHeaders.getOrDefault(Constant.NONCE_HEADER_NAME, "");
        hb.add(Constant.NONCE_HEADER_NAME, flattenHeaders.getOrDefault(Constant.NONCE_HEADER_NAME, ""));
        String stringToSign = stringToSign(request, body, flattenHeaders);
        if (Boolean.TRUE.equals(withToken)) {
            String accessToken = TokenCache.getToken();
            hb.add("access_token", accessToken);
            hb.add("sign", sign(Constant.map.get(Constant.ACCESS_ID), Constant.map.get(Constant.ACCESS_KEY), t, accessToken, nonceStr, stringToSign));
        } else {
            hb.add("sign", sign(Constant.map.get(Constant.ACCESS_ID), Constant.map.get(Constant.ACCESS_KEY), t, nonceStr, stringToSign));
        }
        return hb.build();
    }

    public static String getPathAndSortParam(URL url) {
        String query = url.getQuery();
        String path = url.getPath();
        if (StringUtils.isBlank(query)) {
            return path;
        }
        Map<String, String> kvMap = new TreeMap<>();
        String[] kvs = query.split("\\&");
        for (String kv : kvs) {
            String[] kvArr = kv.split("=");
            if (kvArr.length > 1) {
                kvMap.put(kvArr[0], kvArr[1]);
            } else {
                kvMap.put(kvArr[0], "");
            }
        }
        return path + "?" + kvMap.entrySet().stream().map(it -> it.getKey() + "=" + it.getValue())
                .collect(Collectors.joining("&"));
    }

    private static String stringToSign(Request request, String body, Map<String, String> headers) throws Exception {
        List<String> lines = new ArrayList<>(16);
        lines.add(request.method().toUpperCase());
        String bodyHash = EMPTY_HASH;
        if (request.body() != null && request.body().contentLength() > 0) {
            bodyHash = Sha256Util.encryption(body);
        }
        String signHeaders = headers.get(SING_HEADER_NAME);
        String headerLine = "";
        if (signHeaders != null) {
            String[] sighHeaderNames = signHeaders.split("\\s*:\\s*");
            headerLine = Arrays.stream(sighHeaderNames).map(String::trim)
                    .filter(it -> it.length() > 0)
                    .map(it -> it + ":" + headers.get(it))
                    .collect(Collectors.joining("\n"));
        }
        lines.add(bodyHash);
        lines.add(headerLine);
        String paramSortedPath = getPathAndSortParam(request.url());
        lines.add(paramSortedPath);
        return String.join("\n", lines);
    }

    private static Map<String, String> flattenHeaders(Map<String, String> headers) {
        Map<String, String> newHeaders = new HashMap<>();
        headers.forEach((name, values) -> {
            if (values == null || values.isEmpty()) {
                newHeaders.put(name, "");
            } else {
                newHeaders.put(name, values);
            }
        });
        return newHeaders;
    }

    /**
     * 计算sign
     */
    private static String sign(String accessId, String secret, String t, String accessToken, String nonce, String stringToSign) {
        StringBuilder sb = new StringBuilder();
        sb.append(accessId);
        if (StringUtils.isNotBlank(accessToken)) {
            sb.append(accessToken);
        }
        sb.append(t);
        if (StringUtils.isNotBlank(nonce)) {
            sb.append(nonce);
        }
        sb.append(stringToSign);
        return Sha256Util.sha256HMAC(sb.toString(), secret);
    }

    private static String sign(String accessId, String secret, String t, String nonce, String stringToSign) {
        return sign(accessId, secret, t, NONE_STRING, nonce, stringToSign);
    }

    /**
     * 处理get请求
     */
    public static Request.Builder getRequest(String url) {
        Request.Builder request;
        try {
            request = new Request.Builder()
                    .url(url)
                    .get();
        } catch (IllegalArgumentException e) {
            throw new TuyaCloudSDKException(e.getMessage());
        }
        return request;
    }

    /**
     * 处理post请求
     */
    public static Request.Builder postRequest(String url, String body) {
        Request.Builder request;
        try {
            request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(CONTENT_TYPE, body));
        } catch (IllegalArgumentException e) {
            throw new TuyaCloudSDKException(e.getMessage());
        }

        return request;
    }

    /**
     * 处理put请求
     */
    public static Request.Builder putRequest(String url, String body) {
        Request.Builder request;
        try {
            request = new Request.Builder()
                    .url(url)
                    .put(RequestBody.create(CONTENT_TYPE, body));
        } catch (IllegalArgumentException e) {
            throw new TuyaCloudSDKException(e.getMessage());
        }
        return request;
    }


    /**
     * 处理delete请求
     */
    public static Request.Builder deleteRequest(String url, String body) {
        Request.Builder request;
        try {
            request = new Request.Builder()
                    .url(url)
                    .delete(RequestBody.create(CONTENT_TYPE, body));
        } catch (IllegalArgumentException e) {
            throw new TuyaCloudSDKException(e.getMessage());
        }
        return request;
    }

    /**
     * 执行请求
     */
    public static Response doRequest(Request request) {
        Response response;
        try {
            response = getHttpClient().newCall(request).execute();
        } catch (IOException e) {
            throw new TuyaCloudSDKException(e.getMessage());
        }
        return response;
    }

    /**
     * 获取 http client
     */
    private static OkHttpClient getHttpClient() {
        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(connTimeout, TimeUnit.SECONDS);
        client.setReadTimeout(readTimeout, TimeUnit.SECONDS);
        client.setWriteTimeout(writeTimeout, TimeUnit.SECONDS);

        return client;
    }
}
