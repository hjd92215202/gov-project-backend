package com.gov.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一接口返回结果封装
 */
@Data
public class R<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer code;   // 状态码：200代表成功，500代表失败，401代表未登录等
    private String msg;     // 提示信息
    private T data;         // 实际返回的数据

    // 成功（无数据）
    public static <T> R<T> ok() {
        return restResult(null, 200, "操作成功");
    }

    // 成功（有数据）
    public static <T> R<T> ok(T data) {
        return restResult(data, 200, "操作成功");
    }

    // 成功（有数据有自定义消息）
    public static <T> R<T> ok(T data, String msg) {
        return restResult(data, 200, msg);
    }

    // 失败
    public static <T> R<T> fail(String msg) {
        return restResult(null, 500, msg);
    }

    // 失败（自定义状态码）
    public static <T> R<T> fail(Integer code, String msg) {
        return restResult(null, code, msg);
    }

    private static <T> R<T> restResult(T data, Integer code, String msg) {
        R<T> apiResult = new R<>();
        apiResult.setCode(code);
        apiResult.setData(data);
        apiResult.setMsg(msg);
        return apiResult;
    }
}