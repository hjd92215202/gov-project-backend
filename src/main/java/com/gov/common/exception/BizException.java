package com.gov.common.exception;

/**
 * 统一业务异常。
 * 用于替代裸 RuntimeException，显式携带 HTTP 风格状态码与对外消息。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(400, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
