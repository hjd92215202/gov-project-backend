package com.gov.module.project.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProjectMapVO {
    private Long id;
    private String projectName;
    private String address;
    private String status;

    // ECharts 散点图需要的坐标格式
    private BigDecimal longitude;
    private BigDecimal latitude;

    // 分级字段，用于下钻筛选
    private String province;
    private String city;
    private String district;
}