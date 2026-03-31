package com.gov.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.gov.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.validation.ConstraintViolationException;

/**
 * 全局异常处理器。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public R<String> handleValidationException(Exception e) {
        log.warn("参数校验失败: {}", e.getMessage());
        if (e instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException ex = (MethodArgumentNotValidException) e;
            if (ex.getBindingResult().getFieldError() != null) {
                return R.fail(400, ex.getBindingResult().getFieldError().getDefaultMessage());
            }
        }
        if (e instanceof BindException) {
            BindException ex = (BindException) e;
            if (ex.getBindingResult().getFieldError() != null) {
                return R.fail(400, ex.getBindingResult().getFieldError().getDefaultMessage());
            }
        }
        if (e instanceof ConstraintViolationException) {
            ConstraintViolationException ex = (ConstraintViolationException) e;
            if (!ex.getConstraintViolations().isEmpty()) {
                return R.fail(400, ex.getConstraintViolations().iterator().next().getMessage());
            }
        }
        return R.fail(400, "请求参数校验失败");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public R<String> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return R.fail(400, e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public R<String> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        long maxUploadSize = e.getMaxUploadSize();
        String readableSize = maxUploadSize > 0 ? formatFileSize(maxUploadSize) : "100MB";
        log.warn("上传文件超过限制: {}", readableSize);
        return R.fail(400, "单个附件不能超过 " + readableSize);
    }

    @ExceptionHandler(Exception.class)
    public R<String> handleException(Exception e) {
        log.error("系统发生未捕获异常", e);
        return R.fail(500, "服务器内部错误，请联系管理员");
    }

    @ExceptionHandler(NotLoginException.class)
    public R<String> handleNotLoginException(NotLoginException e) {
        log.warn("用户未登录被拦截: {}", e.getMessage());
        return R.fail(401, "您还未登录或登录已过期，请重新登录");
    }

    @ExceptionHandler(NotPermissionException.class)
    public R<String> handleNotPermissionException(NotPermissionException e) {
        log.warn("用户无权限被拦截: {}", e.getMessage());
        return R.fail(403, "抱歉，您没有操作该功能的权限");
    }

    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + "B";
        }
        if (size < 1024 * 1024) {
            return String.format("%.1fKB", size / 1024.0D);
        }
        if (size < 1024 * 1024 * 1024) {
            return String.format("%.1fMB", size / 1024.0D / 1024.0D);
        }
        return String.format("%.1fGB", size / 1024.0D / 1024.0D / 1024.0D);
    }
}
