package com.gov.module.system.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SysDeptTreeVO {
    private Long id;
    private Long parentId;
    private String deptName;
    private Long leaderId;
    private String leaderName;
    private List<SysDeptTreeVO> children = new ArrayList<>();
}
