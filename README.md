# Tuya Cloud API SDK for JAVA

[English](README.md) | [中文版](README_cn.md)

## Introduction

Tuya Cloud API SDK for JAVA

## Preparation

* Confirm JDK7 version and above

* Register a developer account on the Tuya Cloud platform and determine the values of AccessID, AccessKey, Endpoint (call address)

## Source installation
1. Go to [Github code hosting address](https://registry.code.tuya-inc.top/shengjun.zhang/tuya_cloud_sdk_java) to download the source code compression package.
2. Unzip the source code package to a suitable location for your project.
3. For the reference of the corresponding module code in the code, see the example.

## Reference jar package installation
1. Contact relevant personnel to obtain the jar package.
2. Add the jar package to the appropriate location in your code

## Instructions for use
There are two ways to implement interface requests in the SDK. If you need to implement some interfaces yourself, choose one of them. You can also contact us and we will update in time.

### Implementation of custom classes (recommended)
#### Define the request class
The request class needs to implement the `com.tuya.api.common.ApiRequest` interface, and improve the `getRequestMethod` and `getRequestUrl` methods. If the request needs to pass the body parameter, you need to implement the `com.tuya.api.common.ApiRequestBody` interface again to improve the `getRequestBody` method.

```java
    /**
     * Sync user request class
     */
    public class SyncUserReq implements ApiRequest, ApiRequestBody {
    
        /**
         * Channel identifier
         */
        @NotBlank
        private String schema;
    
        /**
         * User
         */
        @NotNull
        @Valid
        private SyncUserVO user;
    
        public SyncUserReq(String schema, SyncUserVO user) {
            this.schema = schema;
            this.user = user;
        }
    
        public HttpMethod getRequestMethod() {
            return HttpMethod.POST;
        }
    
        public String getRequestUrl() {
            return String.format("/v1.0/apps/%s/user", this.schema);
        }
    
        public String getRequestBody() {
            return new Gson().toJson(this.user);
        }
    }
```

#### Define the client class and expose the request method
```java
    /**
     * User client class
     */
    public class UserClient {
    
        /**
         * Sync users
         *
         * @param schema Channel identifier
         * @param user User data
         * @return
         */
        public static TuyaResult syncUser(String schema, SyncUserVO user) {
            return RequestHandler.sendRequest(new SyncUserReq(schema, user));
        }
    
        /**
         * Get users
         *
         * @param schema Channel identifier
         * @param pageNo The current page, starting from 1
         * @param pageSize Page size
         * @return
         */
        public static TuyaResult getUsers(String schema, int pageNo, int pageSize) {
            return RequestHandler.sendRequest(new GetUsersReq(schema, pageNo, pageSize));
        }
    }
```

#### Call method
```java
    // Initialize developer information, corresponding to Tuya Cloud AccessId, AccessKey, Tuya Cloud service URL
    lientConfig.init(accessId, accessKey, RegionEnum.URL_CN);

    // Example of synchronizing a user
    SyncUserVO vo = new SyncUserVO("86", "17265439876", "17265439876", "1231231", 1);
    UserClient.syncUser(schema, vo);
```
> **Note**: The developer information needs to be initialized before calling the interface.

### General interface
Call CommonClient and pass in the corresponding parameters.
```java
    // Initialize developer information, corresponding to Tuya Cloud AccessId, AccessKey, Tuya Cloud service URL
    lientConfig.init(accessId, accessKey, RegionEnum.URL_CN);

    // Common interface example
    SyncUserVO vo = new SyncUserVO("86", "17265439876", "17265439876", "1231231", 1);
    TuyaResult result = CommonClient.sendRequest("/v1.0/apps/xxx/user", HttpMethod.POST, null, vo);
    System.out.println(result);
```
> **Note**: The developer information needs to be initialized before calling the interface.

## Currently supported API

| Method | API | Description |
| ---- | ---- | ---- |
| TokenClient.getToken | GET /v1.0/token?grant_type=1 | [Get access_token in simple mode](https://docs.tuya.com/docDetail?code=K8uuxenajovgv) |
| TokenClient.refreshToken | GET /v1.0/token/{{easy_refresh_token}} | [Refresh token](https://docs.tuya.com/docDetail?code=K8uuxfcvdsqwm) |
| DeviceClient.getDevice | GET /v1.0/devices/{{device_id}} | [Get device information](https://docs.tuya.com/docDetail?code=K8uuxen89a81x) |
| DeviceClient.getDeviceFunctions | GET /v1.0/devices/{deviceId}/functions | [Get the list of functions supported by the device](https://docs.tuya.com/docDetail?code=K8uuxemwya69p) |
| DeviceClient.getDeviceFunctionByCategory | GET /v1.0/functions/{category} | [Get function list according to category](https://docs.tuya.com/docDetail?code=K8uuxemym7qkt) |
| DeviceClient.getDeviceStatus | GET /v1.0/devices/{{device_id}}/status | [Get device function point information](https://docs.tuya.com/docDetail?code=K8uuxen4ux749) |
| DeviceClient.getDeviceList | GET /v1.0/devices/status?device_ids={{device_id}} | [Batch device status](https://docs.tuya.com/docDetail?code=K8uuxenar6kgc) |
| DeviceClient.postDeviceCommand | POST /v1.0/devices/{{device_id}}/commands | [Device instruction issuance](https://docs.tuya.com/docDetail?code=K8uuxfcxbpwlo) |
| DeviceClient.deleteDevice | DELETE /v1.0/devices/{device_id} | [Remove device](https://docs.tuya.com/docDetail?code=K8uuxemvwtp3z) |
| DeviceClient.generateDeviceToken | POST /v1.0/devices/token | [Generate device configuration token](https://docs.tuya.com/docDetail?code=K8uuxfcujsk6n) |
| DeviceClient.getDevicesByToken | GET /v1.0/devices/tokens/{{pair_token}} | [Get device list based on token](https://docs.tuya.com/docDetail?code=K8uuxemz174o3) |
| DeviceClient.getDeviceListByUid | GET /v1.0/users/{uid}/devices | [Get device list based on user id](https://docs.tuya.com/docDetail?code=K8uuxfcuesrh7) |
| UserClient.syncUser | POST /v1.0/apps/{schema}/user | [Cloud User Registration](https://docs.tuya.com/docDetail?code=K8uuxfcuhc2ei) |
| UserClient.getUsers | GET /v1.0/apps/{schema}/users?page_no=&page_size= | [Get User List](https://docs.tuya.com/docDetail?code=K8uuxemwe9kwb) |

## FAQ

### About refreshToken interface

> **Note**: The refreshToken interface will return a new access_token, even if the old token has not expired.

This logic is already done in the `GetToken` method, and users generally do not need to call the refresh interface.

### Do I need to get the token or refresh the token before calling the API?

No, this layer of logic has been implemented in the  API method. The token information will be cached in memory.

### When calling an interface, if the token has expired, do I need to manually call the refresh-token interface?

No, in the `GetToken()` method implementation, it will check whether the token has expired. If it expires, it will be pulled again.

### If your token will be refreshed in multiple nodes, then you need to implement common.TokenLocalManage interface yourself
The Tuya’s cloud token only guarantees that there will be no problems in refreshing the targeted user level, but the concurrent refresh of a user’s token on multiple nodes will cause only one node to be successful and others to be failed.
Because the `get token` interface will return access_token and refresh_token, but the `refresh token` interface will erase the current refresh_token, and a new one will be invalidated.

### How to deal with the exception information and error of the API method?

If the interface returns an error, it can generally be a url error or a json parsing error, you can contact Tuya related staff to help modify.

If the error is empty, but the `success` field of `response` is false, the developer can check according to the detailed error information in the `Msg` field.

### In the interface for getting device list, if there are multiple deviceIDs, how to splice it?

Multiple deviceIDs, separated by English commas

### In the interface for obtaining user list, what does schema refer to?

After creating the APP-SDK, the channel identifier of the detail page is the schema.

### what does pair_token mean in `v1.0/devices/tokens/{{pair_token}}` interface? How to get it?

Pair_token refers to the network token of a user under the app, which can be obtained from v1.0/devices/token.


## Support

You can get support from Tuya with the following methods:

- Tuya Smart Help Center: [https://support.tuya.com/en/help](https://support.tuya.com/en/help)
- Technical Support Council: [https://iot.tuya.com/council](https://iot.tuya.com/council)

