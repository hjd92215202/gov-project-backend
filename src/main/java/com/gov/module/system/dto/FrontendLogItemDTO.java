package com.gov.module.system.dto;

import lombok.Data;

/**
 * 职责：定义单条前端运行监控日志上报 DTO。
 * 为什么存在：前端上报字段需要和落库实体解耦，便于后端做格式校验和字段裁剪。
 * 关键输入输出：输入为浏览器运行时采集的日志项，输出为控制器内转换后的实体。
 * 关联链路：前端监控上报。
 */
@Data
public class FrontendLogItemDTO {
    /** 日志时间 ISO 字符串。 */
    private String time;
    /** 日志级别。 */
    private String level;
    /** 日志类型。 */
    private String type;
    /** 事件名。 */
    private String eventName;
    /** 核心消息。 */
    private String message;
    /** 页面路径。 */
    private String pagePath;
    /** 链路 ID。 */
    private String traceId;
    /** 详细 JSON 文本。 */
    private String detailJson;
}
