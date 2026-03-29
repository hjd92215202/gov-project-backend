package com.gov.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 职责：表示接口访问审计日志的数据库实体。
 * 为什么存在：为“审计日志页面”和安全排查提供可检索、可分页的数据基座。
 * 关键输入输出：由审计过滤器写入，控制器分页读取并转换为 VO 返回前端。
 * 关联链路：AuditAccessFilter -> sys_audit_log -> /system/audit/page。
 */
@Data
@TableName("sys_audit_log")
public class SysAuditLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 审计日志主键。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 操作用户 ID；未登录请求为 null。 */
    private Long userId;
    /** HTTP 方法，如 GET/POST。 */
    private String requestMethod;
    /** 请求路径。 */
    private String requestUri;
    /** 客户端 IP。 */
    private String clientIp;
    /** 浏览器或客户端 User-Agent。 */
    private String userAgent;
    /** HTTP 状态码。 */
    private Integer httpStatus;
    /** 请求耗时（毫秒）。 */
    private Long durationMs;
    /** 链路追踪 ID。 */
    private String traceId;
    /** 请求到达时间。 */
    private Date requestTime;
}
