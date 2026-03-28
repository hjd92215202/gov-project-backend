package com.gov.module.project.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 项目地图响应 VO。
 * 给地图页使用，重点提供坐标、行政区和轻量展示字段。
 */
@Data
public class ProjectMapVO {
    /** 项目 ID。 */
    private Long id;
    /** 地图提示里展示的项目名称。 */
    private String projectName;
    /** 地图提示里展示的地址。 */
    private String address;
    /** 当前状态文案。 */
    private String status;
    /** 经度，ECharts 散点图使用。 */
    private BigDecimal longitude;
    /** 纬度，ECharts 散点图使用。 */
    private BigDecimal latitude;
    /** 省级行政区。 */
    private String province;
    /** 市级行政区。 */
    private String city;
    /** 区县级行政区。 */
    private String district;
}
