package com.gov.module.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gov.module.project.dto.ProjectAttachmentDTO;
import com.gov.module.project.entity.BizProject;

import java.util.List;

/**
 * 项目服务接口。
 * 用于承接项目领域的统一访问入口。
 */
public interface BizProjectService extends IService<BizProject> {

    /**
     * 创建项目并同步附件。
     *
     * @param project 项目实体
     * @param attachments 附件列表
     */
    void saveProjectWithAttachments(BizProject project, List<ProjectAttachmentDTO> attachments);

    /**
     * 更新项目并同步附件。
     *
     * @param project 项目实体
     * @param attachments 附件列表
     */
    void updateProjectWithAttachments(BizProject project, List<ProjectAttachmentDTO> attachments);

    /**
     * 删除项目及其附件。
     *
     * @param projectId 项目 ID
     */
    void removeProjectWithAttachments(Long projectId);
}
