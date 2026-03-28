package com.gov.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一接口返回结果封装。
 * 这个类存在的意义是让所有前后端交互都遵循同一响应结构，
 * 前端只需要按 `code / msg / data` 一套规则处理即可。
 */
@Data
public class R<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer code;   // 状态码：200代表成功，500代表失败，401代表未登录等
    private String msg;     // 提示信息
    private T data;         // 实际返回的数据

    /**
     * 构造成功响应，不附带业务数据。
     *
     * @param <T> 数据泛型
     * @return 成功响应
     */
    public static <T> R<T> ok() {
        return restResult(null, 200, "操作成功");
    }

    /**
     * 构造成功响应，并携带数据。
     *
     * @param data 响应数据
     * @param <T> 数据泛型
     * @return 成功响应
     */
    public static <T> R<T> ok(T data) {
        return restResult(data, 200, "操作成功");
    }

    /**
     * 构造成功响应，并自定义提示消息。
     *
     * @param data 响应数据
     * @param msg 成功提示语
     * @param <T> 数据泛型
     * @return 成功响应
     */
    public static <T> R<T> ok(T data, String msg) {
        return restResult(data, 200, msg);
    }

    /**
     * 构造失败响应，默认使用 500 业务失败码。
     *
     * @param msg 失败提示语
     * @param <T> 数据泛型
     * @return 失败响应
     */
    public static <T> R<T> fail(String msg) {
        return restResult(null, 500, msg);
    }

    /**
     * 构造失败响应，并自定义状态码。
     *
     * @param code 状态码
     * @param msg 失败提示语
     * @param <T> 数据泛型
     * @return 失败响应
     */
    public static <T> R<T> fail(Integer code, String msg) {
        return restResult(null, code, msg);
    }

    /**
     * 构造统一返回对象的底层方法。
     *
     * @param data 响应数据
     * @param code 状态码
     * @param msg 提示语
     * @param <T> 数据泛型
     * @return 封装后的响应对象
     */
    private static <T> R<T> restResult(T data, Integer code, String msg) {
        R<T> apiResult = new R<>();
        apiResult.setCode(code);
        apiResult.setData(data);
        apiResult.setMsg(msg);
        return apiResult;
    }
}
