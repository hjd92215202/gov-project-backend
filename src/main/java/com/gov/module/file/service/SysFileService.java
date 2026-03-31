package com.gov.module.file.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gov.module.file.entity.SysFile;
import com.gov.module.project.dto.ProjectAttachmentDTO;
import com.gov.module.project.vo.ProjectFileVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;

/**
 * 文件服务接口。
 * 统一收口对象存储上传、附件绑定、详情展示和删除清理逻辑。
 */
public interface SysFileService extends IService<SysFile> {

    /**
     * 上传项目附件并返回前端可直接展示的附件信息。
     *
     * @param file 上传文件
     * @return 附件响应对象
     */
    ProjectFileVO uploadProjectFile(MultipartFile file);

    /**
     * 查询项目附件列表。
     *
     * @param projectId 项目 ID
     * @return 附件列表
     */
    List<ProjectFileVO> listProjectFiles(Long projectId);

    /**
     * 同步项目附件绑定关系。
     * 会把本次提交中保留的附件绑定到项目，并清理已移除的旧附件。
     *
     * @param projectId 项目 ID
     * @param attachments 前端提交的附件列表
     */
    void syncProjectFiles(Long projectId, List<ProjectAttachmentDTO> attachments);

    /**
     * 删除项目全部附件及其对象存储文件。
     *
     * @param projectId 项目 ID
     */
    void removeProjectFiles(Long projectId);

    /**
     * 清理指定的临时附件。
     * 只会删除尚未绑定业务主键的文件，避免误删正式附件。
     *
     * @param fileIds 文件 ID 列表
     * @return 实际清理数量
     */
    int cleanupTemporaryFiles(List<Long> fileIds);

    /**
     * 清理早于指定时间的未绑定临时附件。
     *
     * @param deadline 截止时间
     * @return 实际清理数量
     */
    int cleanupExpiredTemporaryFiles(Date deadline);
}
