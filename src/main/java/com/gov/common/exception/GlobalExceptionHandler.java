package com.gov.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.gov.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 * 负责把后端抛出的异常统一转换成前端可识别的 `R` 响应结构。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 兜底处理所有未显式捕获的异常。
     */
    @ExceptionHandler(Exception.class)
    public R<String> handleException(Exception e) {
        log.error("系统发生未知异常", e);
        return R.fail(500, "服务器内部错误，请联系管理员");
    }

    /**
     * 处理未登录或登录失效异常。
     */
    @ExceptionHandler(NotLoginException.class)
    public R<String> handleNotLoginException(NotLoginException e) {
        log.warn("用户未登录被拦截: {}", e.getMessage());
        return R.fail(401, "您还未登录或登录已过期，请重新登录");
    }

    /**
     * 处理无权限访问异常。
     */
    @ExceptionHandler(NotPermissionException.class)
    public R<String> handleNotPermissionException(NotPermissionException e) {
        log.warn("用户无权限被拦截: {}", e.getMessage());
        return R.fail(403, "抱歉，您没有操作该功能的权限");
    }
}
