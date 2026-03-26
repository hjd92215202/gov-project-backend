package com.gov.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.gov.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 拦截所有未知异常 (兜底)
     */
    @ExceptionHandler(Exception.class)
    public R<String> handleException(Exception e) {
        log.error("系统发生未知异常: ", e);
        return R.fail(500, "服务器内部错误，请联系管理员");
    }

    /**
     * 拦截 Sa-Token 权限异常（未登录）
     */
    @ExceptionHandler(NotLoginException.class)
    public R<String> handleNotLoginException(NotLoginException e) {
        log.warn("用户未登录拦截: {}", e.getMessage());
        return R.fail(401, "您还未登录或登录已过期，请重新登录");
    }

    /**
     * 拦截 Sa-Token 权限异常（无权限）
     */
    @ExceptionHandler(NotPermissionException.class)
    public R<String> handleNotPermissionException(NotPermissionException e) {
        log.warn("用户无权限拦截: {}", e.getMessage());
        return R.fail(403, "抱歉，您没有操作该功能的权限");
    }

    // 后续如果有其他的自定义业务异常，都可以接着在这里加 @ExceptionHandler...
}