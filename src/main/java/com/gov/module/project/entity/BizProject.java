package com.gov.module.project.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gov.crypto.SmTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 项目实体。
 * 对应数据库中的 `biz_project` 表，保存项目在库内的完整结构。
 * 控制器不会直接把它原样返回前端，而是再映射成分页、详情、地图等更明确的响应对象。
 */
@Data
@TableName(value = "biz_project", autoResultMap = true)
public class BizProject {

    /** 项目主键。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 项目名称。 */
    private String projectName;
    /** 项目编号。 */
    private String projectCode;
    /** 项目详细地址。 */
    private String address;

    /** 省级行政区。 */
    private String province;
    /** 市级行政区。 */
    private String city;
    /** 区县级行政区。 */
    private String district;

    /** 经度。 */
    private BigDecimal longitude;
    /** 纬度。 */
    private BigDecimal latitude;

    /** 负责人姓名。 */
    private String leaderName;

    /** 负责人手机号，入库时通过类型处理器做转换。 */
    @TableField(typeHandler = SmTypeHandler.class)
    private String leaderPhone;
    /** 页面提交时临时携带的负责人用户 ID，不直接落库。 */
    @TableField(exist = false)
    private Long leaderUserId;

    /** 项目说明。 */
    private String description;
    /** 项目状态：0待提交、1审批中、2已通过、3已驳回。 */
    private Integer status;
    /** 创建人用户 ID。 */
    private Long creatorId;
    /** 创建人所属部门 ID。 */
    private Long creatorDeptId;

    /** 逻辑删除标记。 */
    @TableLogic
    private Integer deleted;

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新时间。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
