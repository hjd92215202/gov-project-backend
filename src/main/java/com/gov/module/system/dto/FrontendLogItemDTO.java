package com.gov.module.system.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 前端监控单条日志 DTO。
 */
@Data
public class FrontendLogItemDTO {

    @Size(max = 64, message = "日志时间长度不能超过64个字符")
    private String time;

    @Size(max = 16, message = "日志级别长度不能超过16个字符")
    private String level;

    @Size(max = 32, message = "日志类型长度不能超过32个字符")
    private String type;

    @Size(max = 64, message = "事件名称长度不能超过64个字符")
    private String eventName;

    @Size(max = 255, message = "日志消息长度不能超过255个字符")
    private String message;

    @Size(max = 255, message = "页面路径长度不能超过255个字符")
    private String pagePath;

    @Size(max = 64, message = "链路标识长度不能超过64个字符")
    private String traceId;

    @Size(max = 4000, message = "日志详情长度不能超过4000个字符")
    private String detailJson;
}
