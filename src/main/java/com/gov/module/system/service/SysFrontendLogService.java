package com.gov.module.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gov.module.system.entity.SysFrontendLog;

/**
 * 职责：定义前端监控日志读写服务契约。
 * 为什么存在：隔离控制器对底层 Mapper 的直接依赖，保持系统监控链路结构统一。
 * 关键输入输出：输入为前端监控实体或查询条件，输出为保存结果与分页数据。
 * 关联链路：/system/frontend-monitor/report、/system/frontend-monitor/page。
 */
public interface SysFrontendLogService extends IService<SysFrontendLog> {
}
