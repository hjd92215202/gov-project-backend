package com.gov.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gov.module.system.entity.SysFrontendLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 职责：承载前端运行监控日志表 `sys_frontend_log` 的基础数据库访问能力。
 * 为什么存在：让前端监控日志写入与分页查询复用 MyBatis-Plus 通用能力。
 * 关键输入输出：输入为前端监控实体，输出为持久化结果或分页记录。
 * 关联链路：前端日志上报接口、前端监控页面。
 */
@Mapper
public interface SysFrontendLogMapper extends BaseMapper<SysFrontendLog> {
}
