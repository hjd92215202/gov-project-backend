package com.gov.module.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gov.module.file.service.SysFileService;
import com.gov.module.project.dto.ProjectAttachmentDTO;
import com.gov.module.project.entity.BizProject;
import com.gov.module.project.mapper.BizProjectMapper;
import com.gov.module.project.service.BizProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 项目服务实现。
 * 当前主要复用 MyBatis-Plus 提供的通用 CRUD 能力，
 * 后续若项目领域逻辑继续下沉，可以直接在这里扩展。
 */
@Service
public class BizProjectServiceImpl extends ServiceImpl<BizProjectMapper, BizProject> implements BizProjectService {

    @Autowired
    private SysFileService sysFileService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveProjectWithAttachments(BizProject project, List<ProjectAttachmentDTO> attachments) {
        save(project);
        sysFileService.syncProjectFiles(project.getId(), attachments);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProjectWithAttachments(BizProject project, List<ProjectAttachmentDTO> attachments) {
        updateById(project);
        sysFileService.syncProjectFiles(project.getId(), attachments);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeProjectWithAttachments(Long projectId) {
        sysFileService.removeProjectFiles(projectId);
        removeById(projectId);
    }
}
