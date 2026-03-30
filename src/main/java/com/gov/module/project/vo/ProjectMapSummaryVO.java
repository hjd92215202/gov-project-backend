package com.gov.module.project.vo;

import lombok.Data;

/**
 * 地图汇总响应 VO。
 * 用于省级、市级地图按行政区返回项目数量汇总，减少前端在首屏和下钻阶段拉取全量点位。
 */
@Data
public class ProjectMapSummaryVO {

    /** 汇总层级，当前支持 city / district。 */
    private String regionLevel;

    /** 行政区名称。 */
    private String regionName;

    /** 审批通过项目数。 */
    private Long projectCount;
}
