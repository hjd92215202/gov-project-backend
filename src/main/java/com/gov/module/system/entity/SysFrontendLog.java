package com.gov.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 职责：表示浏览器侧上报的前端运行监控日志实体。
 * 为什么存在：把前端错误、慢路由、慢请求和关键异常统一沉淀到后端，方便管理员集中排查。
 * 关键输入输出：由前端监控上报接口写入，监控页面分页读取并转换为 VO 返回前端。
 * 关联链路：logger/front-monitor -> /system/frontend-monitor/report -> sys_frontend_log -> 管理员监控页面。
 */
@Data
@TableName("sys_frontend_log")
public class SysFrontendLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 日志主键。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 操作用户 ID，匿名场景可为空。 */
    private Long userId;
    /** 日志级别：debug/info/warn/error。 */
    private String logLevel;
    /** 日志类型：request/route/runtime_error/action/app。 */
    private String logType;
    /** 事件名，如 menu_skip、slow_request。 */
    private String eventName;
    /** 页面展示的核心消息。 */
    private String message;
    /** 发生问题时的页面路径。 */
    private String pagePath;
    /** 前后端贯通链路 ID。 */
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
