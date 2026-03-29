package com.gov.module.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gov.module.system.entity.SysFrontendLog;
import com.gov.module.system.mapper.SysFrontendLogMapper;
import com.gov.module.system.service.SysFrontendLogService;
import org.springframework.stereotype.Service;

/**
 * 职责：前端监控日志服务默认实现。
 * 为什么存在：复用 MyBatis-Plus 的通用 Service 能力，减少样板代码。
 * 关键输入输出：输入为前端监控日志实体与查询条件，输出为数据库读写结果。
 * 关联链路：前端上报、管理员监控页面。
 */
@Service
public class SysFrontendLogServiceImpl extends ServiceImpl<SysFrontendLogMapper, SysFrontendLog> implements SysFrontendLogService {
}
