package com.gov.module.file.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 文件实体，对应 sys_file 表。
 */
@Data
@TableName("sys_file")
public class SysFile {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long bizId;

    private String fileName;

    private String filePath;

    private String fileType;

    private Long fileSize;

    private Long creatorUserId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
