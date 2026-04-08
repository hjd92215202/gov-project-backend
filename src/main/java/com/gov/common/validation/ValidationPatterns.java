package com.gov.common.validation;

/**
 * 统一维护常用校验正则，避免多处散落硬编码。
 */
public final class ValidationPatterns {

    public static final String PHONE = "^[0-9-]{7,20}$";
    public static final String USERNAME = "^[A-Za-z0-9_]{3,32}$";
    public static final String DISPLAY_NAME = "^[A-Za-z0-9\\u4e00-\\u9fa5·\\s]{2,30}$";
    public static final String PROJECT_CODE = "^[A-Za-z0-9_-]{2,64}$";
    public static final String MENU_KEY = "^[a-z0-9:_-]{1,64}$";

    private ValidationPatterns() {
    }
}
