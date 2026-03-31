package com.gov.module.file.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 文件实体。
 * 对应 `sys_file` 表，当前主要承接项目附件元数据。
 */
@Data
@TableName("sys_file")
public class SysFile {

    /** 文件主键。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联业务主键，当前为项目 ID。 */
    private Long bizId;

    /** 文件原始名称。 */
    private String fileName;

    /** 文件在对象存储中的对象键。 */
    private String filePath;

    /** 文件类型，优先保存 MIME 类型。 */
    private String fileType;

    /** 文件大小，单位字节。 */
    private Long fileSize;

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
