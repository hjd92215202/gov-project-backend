package com.gov.module.system.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 前端监控批量上报 DTO。
 */
@Data
public class FrontendLogReportDTO {

    @Valid
    @Size(max = 50, message = "前端监控单次上报不能超过50条")
    private List<FrontendLogItemDTO> logs;
}
