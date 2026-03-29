package com.gov.module.system.dto;

import lombok.Data;

import java.util.List;

/**
 * 职责：定义前端监控日志批量上报 DTO。
 * 为什么存在：浏览器需要批量发送日志，避免频繁逐条请求增加网络开销。
 * 关键输入输出：输入为日志项列表，输出为控制器内批量落库操作。
 * 关联链路：前端监控上报。
 */
@Data
public class FrontendLogReportDTO {
    /** 批量上报的日志项集合。 */
    private List<FrontendLogItemDTO> logs;
}
