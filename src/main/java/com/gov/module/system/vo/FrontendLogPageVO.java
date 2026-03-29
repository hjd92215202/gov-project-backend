package com.gov.module.system.vo;

import lombok.Data;

import java.util.Date;

/**
 * 职责：定义前端监控分页响应给前端展示的字段。
 * 为什么存在：避免直接暴露数据库实体，稳定监控页面契约并便于字段裁剪。
 * 关键输入输出：输入为 `SysFrontendLog` + 用户信息补充，输出为列表展示 VO。
 * 关联链路：/system/frontend-monitor/page -> 系统设置 -> 前端监控页面。
 */
@Data
public class FrontendLogPageVO {
    /** 日志主键。 */
    private Long id;
    /** 操作用户 ID。 */
    private Long userId;
    /** 用户名。 */
    private String username;
    /** 真实姓名。 */
    private String realName;
    /** 用户部门名称。 */
    private String deptName;
    /** 日志级别。 */
    private String logLevel;
    /** 日志类型。 */
    private String logType;
    /** 事件名。 */
    private String eventName;
    /** 核心消息。 */
    private String message;
    /** 页面路径。 */
    private String pagePath;
    /** 链路 ID。 */
    private String traceId;
    /** 扩展细节 JSON。 */
    private String detailJson;
    /** 客户端真实 IP。 */
    private String clientIp;
    /** 浏览器标识。 */
    private String userAgent;
    /** 上报时间。 */
    private Date createdTime;
}
