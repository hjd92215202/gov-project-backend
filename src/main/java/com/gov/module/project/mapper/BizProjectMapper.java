package com.gov.module.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gov.module.project.entity.BizProject;
import com.gov.module.project.vo.ProjectMapSummaryVO;
import com.gov.module.project.vo.ProjectMapVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 职责：承接项目地图相关的定制查询。
 * 为什么存在：地图页只需要轻量字段和汇总结果，直接走定制 SQL
 * 可以减少实体装配与不必要的排序开销。
 */
@Mapper
public interface BizProjectMapper extends BaseMapper<BizProject> {

    String APPROVED_PROJECT_SCOPE_SQL =
            "FROM biz_project " +
            "WHERE deleted = 0 " +
            "  AND status = 2 " +
            "  <if test='province != null and province != \"\"'>AND province = #{province}</if> " +
            "  <if test='city != null and city != \"\"'>AND city = #{city}</if> " +
            "  <if test='district != null and district != \"\"'>AND district = #{district}</if> " +
            "  <if test='scopeMode == 1 and scopeDeptId != null'>AND creator_dept_id = #{scopeDeptId}</if> " +
            "  <if test='scopeMode == 2 and scopeUserId != null'>AND creator_id = #{scopeUserId}</if> ";

    String NON_BLANK_REGION_SQL =
            "  <choose>" +
            "    <when test='level == \"city\"'>AND city IS NOT NULL AND city != ''</when>" +
            "    <otherwise>AND district IS NOT NULL AND district != ''</otherwise>" +
            "  </choose> ";

    /**
     * 查询地图点位列表。
     * 这里不再追加 create_time 排序，避免地图点位大列表在数据库端额外排序。
     */
    @Select({
            "<script>",
            "SELECT",
            "  id,",
            "  project_name AS projectName,",
            "  address,",
            "  longitude,",
            "  latitude,",
            "  province,",
            "  city,",
            "  district",
            APPROVED_PROJECT_SCOPE_SQL,
            "</script>"
    })
    List<ProjectMapVO> selectApprovedMapList(@Param("province") String province,
                                             @Param("city") String city,
                                             @Param("district") String district,
                                             @Param("scopeMode") Integer scopeMode,
                                             @Param("scopeDeptId") Long scopeDeptId,
                                             @Param("scopeUserId") Long scopeUserId);

    /**
     * 查询地图汇总数据，用于省级按市、市级按区县展示。
     */
    @Select({
            "<script>",
            "SELECT",
            "  #{level} AS regionLevel,",
            "  <choose>",
            "    <when test='level == \"city\"'>city</when>",
            "    <otherwise>district</otherwise>",
            "  </choose> AS regionName,",
            "  COUNT(1) AS projectCount",
            APPROVED_PROJECT_SCOPE_SQL,
            NON_BLANK_REGION_SQL,
            "GROUP BY",
            "  <choose>",
            "    <when test='level == \"city\"'>city</when>",
            "    <otherwise>district</otherwise>",
            "  </choose>",
            "ORDER BY projectCount DESC, regionName ASC",
            "</script>"
    })
    List<ProjectMapSummaryVO> selectApprovedMapSummary(@Param("level") String level,
                                                       @Param("province") String province,
                                                       @Param("city") String city,
                                                       @Param("district") String district,
                                                       @Param("scopeMode") Integer scopeMode,
                                                       @Param("scopeDeptId") Long scopeDeptId,
                                                       @Param("scopeUserId") Long scopeUserId);
}
