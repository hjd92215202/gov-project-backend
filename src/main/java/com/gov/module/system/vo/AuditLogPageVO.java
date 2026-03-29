package com.gov.module.system.vo;

import lombok.Data;

import java.util.Date;

/**
 * 职责：定义审计日志分页响应给前端展示的字段。
 * 为什么存在：避免直接暴露数据库实体，稳定接口契约并便于字段裁剪。
 * 关键输入输出：输入为 `SysAuditLog` + 用户信息补充，输出为列表展示 VO。
 * 关联链路：/system/audit/page -> 审计日志管理页。
 */
@Data
public class AuditLogPageVO {
    /** 审计日志主键。 */
    private Long id;
    /** 操作用户 ID。 */
    private Long userId;
    /** 用户名。 */
    private String username;
    /** 真实姓名。 */
    private String realName;
    /** HTTP 方法。 */
    private String requestMethod;
    /** 请求路径。 */
    private String requestUri;
    /** 客户端 IP。 */
    private String clientIp;
    /** HTTP 状态码。 */
    private Integer httpStatus;
    /** 请求耗时（毫秒）。 */
    private Long durationMs;
    /** 链路追踪 ID。 */
    private String traceId;
    /** 用户代理。 */
    private String userAgent;
    /** 请求时间。 */
    private Date requestTime;
}
