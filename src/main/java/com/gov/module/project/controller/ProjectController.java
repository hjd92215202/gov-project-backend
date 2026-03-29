package com.gov.module.project.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gov.common.result.R;
import com.gov.module.flow.service.FlowService;
import com.gov.module.project.dto.ProjectCreateDTO;
import com.gov.module.project.dto.ProjectSubmitDTO;
import com.gov.module.project.dto.ProjectUpdateDTO;
import com.gov.module.project.entity.BizProject;
import com.gov.module.project.service.BizProjectService;
import com.gov.module.project.vo.ProjectDetailVO;
import com.gov.module.project.vo.ProjectMapVO;
import com.gov.module.project.vo.ProjectPageVO;
import com.gov.module.system.entity.SysDept;
import com.gov.module.system.entity.SysUser;
import com.gov.module.system.service.SysDeptService;
import com.gov.module.system.service.SysUserService;
import com.gov.module.system.vo.UserAccessContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 项目管理接口控制器。
 * 这个类负责承接前端对项目的增删改查、提交审批和地图点位查询请求，
 * 同时在接口层完成项目状态机与数据权限的第一层校验。
 */
@Api(tags = "项目管理")
@RestController
@RequestMapping("/project")
public class ProjectController {

    private static final Pattern CONTACT_PHONE_PATTERN = Pattern.compile("^[0-9-]{7,20}$");

    @Autowired
    private BizProjectService bizProjectService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysDeptService sysDeptService;

    @Autowired
    private FlowService flowService;

    /**
     * 新增项目草稿。
     *
     * @param payload 项目创建 DTO
     * @return 创建结果
     */
    @ApiOperation("新增项目")
    @PostMapping("/add")
    public R<String> add(@RequestBody ProjectCreateDTO payload) {
        BizProject project = toProjectEntity(payload);
        if (StrUtil.isBlank(project.getProjectName())) {
            return R.fail("项目名称不能为空");
        }
        if (project.getStatus() == null) {
            project.setStatus(0);
        }

        Long currentUserId = StpUtil.getLoginIdAsLong();
        SysUser currentUser = sysUserService.getById(currentUserId);
        if (currentUser == null) {
            return R.fail(403, "当前登录用户不存在");
        }
        if (project.getCreatorId() == null) {
            project.setCreatorId(currentUserId);
        }
        if (project.getCreatorDeptId() == null) {
            project.setCreatorDeptId(currentUser.getDeptId());
        }

        UserAccessContext accessContext = currentAccessContext();
        String leaderError = fillLeaderByPermission(project, currentUser, accessContext);
        if (leaderError != null) {
            return R.fail(403, leaderError);
        }
        String phoneError = validateLeaderPhone(project.getLeaderPhone());
        if (phoneError != null) {
            return R.fail(phoneError);
        }

        bizProjectService.save(project);
        return R.ok("项目创建成功");
    }

    /**
     * 项目分页查询。
     * 该接口会结合当前登录人的权限上下文自动裁剪数据范围。
     *
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param projectName 项目名称筛选
     * @param status 状态筛选
     * @param province 省份筛选
     * @param city 城市筛选
     * @param district 区县筛选
     * @return 项目分页结果
     */
    @ApiOperation("项目分页查询")
    @GetMapping("/page")
    public R<IPage<ProjectPageVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district
    ) {
        LambdaQueryWrapper<BizProject> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(
                BizProject::getId,
                BizProject::getProjectName,
                BizProject::getProjectCode,
                BizProject::getAddress,
                BizProject::getProvince,
                BizProject::getCity,
                BizProject::getDistrict,
                BizProject::getLongitude,
                BizProject::getLatitude,
                BizProject::getLeaderName,
                BizProject::getLeaderPhone,
                BizProject::getStatus,
                BizProject::getCreatorId,
                BizProject::getCreatorDeptId,
                BizProject::getCreateTime
        );
        queryWrapper.like(StrUtil.isNotBlank(projectName), BizProject::getProjectName, projectName);
        queryWrapper.eq(status != null, BizProject::getStatus, status);
        queryWrapper.eq(StrUtil.isNotBlank(province), BizProject::getProvince, province);
        queryWrapper.eq(StrUtil.isNotBlank(city), BizProject::getCity, city);
        queryWrapper.eq(StrUtil.isNotBlank(district), BizProject::getDistrict, district);

        if (!applyProjectScope(queryWrapper, currentAccessContext())) {
            return R.fail(403, "当前用户未绑定部门");
        }

        queryWrapper.orderByDesc(BizProject::getCreateTime);
        IPage<BizProject> result = bizProjectService.page(new Page<>(pageNum, pageSize), queryWrapper);
        Page<ProjectPageVO> responsePage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<ProjectPageVO> records = new ArrayList<>();
        for (BizProject item : result.getRecords()) {
            ProjectPageVO vo = new ProjectPageVO();
            vo.setId(item.getId());
            vo.setProjectName(item.getProjectName());
            vo.setProjectCode(item.getProjectCode());
            vo.setAddress(item.getAddress());
            vo.setProvince(item.getProvince());
            vo.setCity(item.getCity());
            vo.setDistrict(item.getDistrict());
            vo.setLongitude(item.getLongitude());
            vo.setLatitude(item.getLatitude());
            vo.setLeaderName(item.getLeaderName());
            vo.setLeaderPhone(item.getLeaderPhone());
            vo.setStatus(item.getStatus());
            vo.setCreatorId(item.getCreatorId());
            vo.setCreatorDeptId(item.getCreatorDeptId());
            vo.setCreateTime(item.getCreateTime());
            records.add(vo);
        }
        responsePage.setRecords(records);
        return R.ok(responsePage);
    }

    /**
     * 查询单个项目详情。
     *
     * @param id 项目 ID
     * @return 项目详情
     */
    @ApiOperation("项目详情")
    @GetMapping("/get/{id}")
    public R<ProjectDetailVO> get(@PathVariable Long id) {
        BizProject project = bizProjectService.getById(id);
        if (project == null) {
            return R.fail("项目不存在");
        }
        if (!canOperateProject(project, currentAccessContext())) {
            return R.fail(403, "无权限查看该项目");
        }
        return R.ok(toProjectDetailVO(project));
    }

    /**
     * 更新项目。
     * 只有草稿和已驳回状态允许编辑，并且必须满足当前数据权限范围。
     *
     * @param payload 项目更新 DTO
     * @return 更新结果
     */
    @ApiOperation("更新项目")
    @PutMapping("/update")
    public R<String> update(@RequestBody ProjectUpdateDTO payload) {
        BizProject project = toProjectEntity(payload);
        if (project.getId() == null) {
            return R.fail("项目ID不能为空");
        }
        if (StrUtil.isBlank(project.getProjectName())) {
            return R.fail("项目名称不能为空");
        }

        BizProject dbProject = bizProjectService.getById(project.getId());
        if (dbProject == null) {
            return R.fail("项目不存在");
        }
        if (!isEditableStatus(dbProject.getStatus())) {
            return R.fail(403, "仅草稿和驳回状态项目可编辑");
        }

        UserAccessContext accessContext = currentAccessContext();
        if (!canOperateProject(dbProject, accessContext)) {
            return R.fail(403, "无权限编辑该项目");
        }

        Long currentUserId = StpUtil.getLoginIdAsLong();
        SysUser currentUser = sysUserService.getById(currentUserId);
        if (currentUser == null) {
            return R.fail(403, "当前登录用户不存在");
        }

        if (dbProject.getCreatorId() == null) {
            dbProject.setCreatorId(currentUserId);
            dbProject.setCreatorDeptId(currentUser.getDeptId());
        }
        if (project.getCreatorId() == null) {
            project.setCreatorId(dbProject.getCreatorId());
        }
        if (project.getCreatorDeptId() == null) {
            project.setCreatorDeptId(dbProject.getCreatorDeptId());
        }
        project.setStatus(dbProject.getStatus());

        String leaderError = fillLeaderByPermission(project, currentUser, accessContext);
        if (leaderError != null) {
            return R.fail(403, leaderError);
        }
        String phoneError = validateLeaderPhone(project.getLeaderPhone());
        if (phoneError != null) {
            return R.fail(phoneError);
        }

        bizProjectService.updateById(project);
        return R.ok("项目更新成功");
    }

    /**
     * 删除项目。
     *
     * @param id 项目 ID
     * @return 删除结果
     */
    @ApiOperation("删除项目")
    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id) {
        BizProject dbProject = bizProjectService.getById(id);
        if (dbProject == null) {
            return R.fail("项目不存在");
        }
        UserAccessContext accessContext = currentAccessContext();
        if (!canDeleteStatus(dbProject.getStatus(), accessContext.isAdmin())) {
            return R.fail(403, "仅草稿和驳回状态项目可删除，管理员可删除已通过项目");
        }
        if (!canOperateProject(dbProject, accessContext)) {
            return R.fail(403, "无权限删除该项目");
        }

        bizProjectService.removeById(id);
        return R.ok("项目删除成功");
    }

    /**
     * 提交项目进入审批流程。
     * 支持“已有草稿提交”和“创建后立即提交流程”两种路径。
     *
     * @param payload 提交 DTO
     * @return 提交结果
     */
    @ApiOperation("提交项目审批")
    @PostMapping("/submit")
    public R<String> submit(@RequestBody ProjectSubmitDTO payload) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        SysUser currentUser = sysUserService.getById(currentUserId);
        if (currentUser == null) {
            return R.fail("当前登录用户不存在");
        }

        UserAccessContext accessContext = currentAccessContext();
        Long projectId;
        BizProject submitProject;

        if (payload.getId() != null) {
            BizProject dbProject = bizProjectService.getById(payload.getId());
            if (dbProject == null) {
                return R.fail("项目不存在");
            }
            if (!isEditableStatus(dbProject.getStatus())) {
                return R.fail(403, "仅草稿和驳回状态项目可提交审批");
            }
            if (!canOperateProject(dbProject, accessContext)) {
                return R.fail(403, "无权限提交该项目审批");
            }

            submitProject = new BizProject();
            submitProject.setId(dbProject.getId());
            submitProject.setStatus(1);
            projectId = dbProject.getId();
            bizProjectService.updateById(submitProject);
        } else {
            if (StrUtil.isBlank(payload.getProjectName())) {
                return R.fail("项目名称不能为空");
            }
            BizProject createProject = toProjectEntity(payload);
            if (createProject.getCreatorId() == null) {
                createProject.setCreatorId(currentUserId);
            }
            if (createProject.getCreatorDeptId() == null) {
                createProject.setCreatorDeptId(currentUser.getDeptId());
            }
            String leaderError = fillLeaderByPermission(createProject, currentUser, accessContext);
            if (leaderError != null) {
                return R.fail(403, leaderError);
            }
            String phoneError = validateLeaderPhone(createProject.getLeaderPhone());
            if (phoneError != null) {
                return R.fail(phoneError);
            }
            createProject.setStatus(1);
            bizProjectService.save(createProject);
            projectId = createProject.getId();
        }

        if (currentUser.getDeptId() == null) {
            return R.fail("当前用户未绑定部门");
        }

        SysDept dept = sysDeptService.getById(currentUser.getDeptId());
        if (dept == null || dept.getLeaderId() == null) {
            return R.fail("部门负责人未配置");
        }
        if (sysUserService.getById(dept.getLeaderId()) == null) {
            return R.fail("部门负责人用户不存在");
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("currentAssignee", dept.getLeaderId().toString());
        flowService.startProcess(String.valueOf(projectId), vars);
        return R.ok("提交审批成功");
    }

    /**
     * 获取地图点位列表。
     * 该接口默认只返回已审批通过的项目，并按当前数据权限裁剪可见范围。
     *
     * @param province 省份筛选
     * @param city 城市筛选
     * @param district 区县筛选
     * @return 地图点位列表
     */
    @ApiOperation("地图点位列表")
    @GetMapping("/map/list")
    public R<List<ProjectMapVO>> getMapList(
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district
    ) {
        LambdaQueryWrapper<BizProject> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(
                BizProject::getId,
                BizProject::getProjectName,
                BizProject::getAddress,
                BizProject::getLongitude,
                BizProject::getLatitude,
                BizProject::getProvince,
                BizProject::getCity,
                BizProject::getDistrict
        );
        queryWrapper.eq(StrUtil.isNotBlank(province), BizProject::getProvince, province);
        queryWrapper.eq(StrUtil.isNotBlank(city), BizProject::getCity, city);
        queryWrapper.eq(StrUtil.isNotBlank(district), BizProject::getDistrict, district);
        queryWrapper.eq(BizProject::getStatus, 2);

        if (!applyProjectScope(queryWrapper, currentAccessContext())) {
            return R.ok(new ArrayList<>());
        }

        List<ProjectMapVO> result = new ArrayList<>();
        for (BizProject item : bizProjectService.list(queryWrapper)) {
            ProjectMapVO vo = new ProjectMapVO();
            vo.setId(item.getId());
            vo.setProjectName(item.getProjectName());
            vo.setAddress(item.getAddress());
            vo.setLongitude(item.getLongitude());
            vo.setLatitude(item.getLatitude());
            vo.setProvince(item.getProvince());
            vo.setCity(item.getCity());
            vo.setDistrict(item.getDistrict());
            result.add(vo);
        }
        return R.ok(result);
    }

    /**
     * 判断当前用户是否有权操作该项目。
     *
     * @param project 项目实体
     * @param accessContext 当前访问上下文
     * @return 是否可操作
     */
    private boolean canOperateProject(BizProject project, UserAccessContext accessContext) {
        if (accessContext.isAdmin()) {
            return true;
        }
        if (accessContext.isDeptLeader()) {
            return accessContext.getDeptId() != null
                    && project.getCreatorDeptId() != null
                    && Objects.equals(accessContext.getDeptId(), project.getCreatorDeptId());
        }
        return Objects.equals(project.getCreatorId(), accessContext.getUserId());
    }

    /**
     * 把当前登录人的数据权限范围应用到项目查询条件上。
     *
     * @param queryWrapper 查询构造器
     * @param accessContext 当前访问上下文
     * @return 是否成功应用范围
     */
    private boolean applyProjectScope(LambdaQueryWrapper<BizProject> queryWrapper, UserAccessContext accessContext) {
        if (accessContext.isAdmin()) {
            return true;
        }
        if (accessContext.isDeptLeader()) {
            if (accessContext.getDeptId() == null) {
                return false;
            }
            queryWrapper.eq(BizProject::getCreatorDeptId, accessContext.getDeptId());
            return true;
        }
        queryWrapper.eq(BizProject::getCreatorId, accessContext.getUserId());
        return true;
    }

    /**
     * 按当前权限规则补齐或校验项目负责人信息。
     * 这是项目新增、更新、提交时的公共权限规则收口点。
     *
     * @param project 项目实体
     * @param currentUser 当前登录用户
     * @param accessContext 当前访问上下文
     * @return 校验失败时返回错误消息，成功返回 null
     */
    private String fillLeaderByPermission(BizProject project, SysUser currentUser, UserAccessContext accessContext) {
        if (currentUser == null) {
            return "当前登录用户不存在";
        }

        Long leaderUserId = project.getLeaderUserId();
        if (leaderUserId != null) {
            if (!accessContext.isAdmin() && !accessContext.isDeptLeader()
                    && !Objects.equals(currentUser.getId(), leaderUserId)) {
                return "普通用户只能将自己设为项目负责人";
            }

            SysUser leaderUser = sysUserService.getById(leaderUserId);
            if (leaderUser == null || !Objects.equals(leaderUser.getStatus(), 1)) {
                return "项目负责人用户不存在或已禁用";
            }
            if (accessContext.isDeptLeader() && !accessContext.isAdmin()) {
                if (currentUser.getDeptId() == null || !Objects.equals(currentUser.getDeptId(), leaderUser.getDeptId())) {
                    return "部门负责人仅可指定本部门人员为负责人";
                }
            }

            project.setLeaderName(StrUtil.isNotBlank(leaderUser.getRealName()) ? leaderUser.getRealName() : leaderUser.getUsername());
            if (StrUtil.isBlank(project.getLeaderPhone()) && StrUtil.isNotBlank(leaderUser.getPhone())) {
                project.setLeaderPhone(leaderUser.getPhone());
            }
            return null;
        }

        if (!accessContext.isAdmin() && !accessContext.isDeptLeader()) {
            if (StrUtil.isBlank(project.getLeaderName())) {
                project.setLeaderName(StrUtil.isNotBlank(currentUser.getRealName()) ? currentUser.getRealName() : currentUser.getUsername());
            }
            if (StrUtil.isBlank(project.getLeaderPhone()) && StrUtil.isNotBlank(currentUser.getPhone())) {
                project.setLeaderPhone(currentUser.getPhone());
            }
        }
        return null;
    }

    /**
     * 把项目实体转换为详情 VO。
     *
     * @param project 项目实体
     * @return 详情 VO
     */
    private ProjectDetailVO toProjectDetailVO(BizProject project) {
        ProjectDetailVO vo = new ProjectDetailVO();
        if (project == null) {
            return vo;
        }
        vo.setId(project.getId());
        vo.setProjectName(project.getProjectName());
        vo.setProjectCode(project.getProjectCode());
        vo.setAddress(project.getAddress());
        vo.setProvince(project.getProvince());
        vo.setCity(project.getCity());
        vo.setDistrict(project.getDistrict());
        vo.setLongitude(project.getLongitude());
        vo.setLatitude(project.getLatitude());
        vo.setLeaderUserId(project.getLeaderUserId());
        vo.setLeaderName(project.getLeaderName());
        vo.setLeaderPhone(project.getLeaderPhone());
        vo.setDescription(project.getDescription());
        vo.setStatus(project.getStatus());
        vo.setCreatorDeptId(project.getCreatorDeptId());
        return vo;
    }

    /**
     * 把创建 DTO 转为项目实体。
     *
     * @param payload 创建 DTO
     * @return 项目实体
     */
    private BizProject toProjectEntity(ProjectCreateDTO payload) {
        BizProject project = new BizProject();
        if (payload == null) {
            return project;
        }
        project.setProjectName(payload.getProjectName());
        project.setProjectCode(payload.getProjectCode());
        project.setAddress(payload.getAddress());
        project.setProvince(payload.getProvince());
        project.setCity(payload.getCity());
        project.setDistrict(payload.getDistrict());
        project.setLongitude(payload.getLongitude());
        project.setLatitude(payload.getLatitude());
        project.setLeaderUserId(payload.getLeaderUserId());
        project.setLeaderName(payload.getLeaderName());
        project.setLeaderPhone(payload.getLeaderPhone());
        project.setDescription(payload.getDescription());
        project.setStatus(payload.getStatus());
        project.setCreatorId(payload.getCreatorId());
        project.setCreatorDeptId(payload.getCreatorDeptId());
        return project;
    }

    /**
     * 把更新 DTO 转为项目实体。
     *
     * @param payload 更新 DTO
     * @return 项目实体
     */
    private BizProject toProjectEntity(ProjectUpdateDTO payload) {
        BizProject project = new BizProject();
        if (payload == null) {
            return project;
        }
        project.setId(payload.getId());
        project.setProjectName(payload.getProjectName());
        project.setProjectCode(payload.getProjectCode());
        project.setAddress(payload.getAddress());
        project.setProvince(payload.getProvince());
        project.setCity(payload.getCity());
        project.setDistrict(payload.getDistrict());
        project.setLongitude(payload.getLongitude());
        project.setLatitude(payload.getLatitude());
        project.setLeaderUserId(payload.getLeaderUserId());
        project.setLeaderName(payload.getLeaderName());
        project.setLeaderPhone(payload.getLeaderPhone());
        project.setDescription(payload.getDescription());
        project.setStatus(payload.getStatus());
        project.setCreatorId(payload.getCreatorId());
        project.setCreatorDeptId(payload.getCreatorDeptId());
        return project;
    }

    /**
     * 把提交 DTO 转为项目实体。
     *
     * @param payload 提交 DTO
     * @return 项目实体
     */
    private BizProject toProjectEntity(ProjectSubmitDTO payload) {
        BizProject project = new BizProject();
        if (payload == null) {
            return project;
        }
        project.setId(payload.getId());
        project.setProjectName(payload.getProjectName());
        project.setProjectCode(payload.getProjectCode());
        project.setAddress(payload.getAddress());
        project.setProvince(payload.getProvince());
        project.setCity(payload.getCity());
        project.setDistrict(payload.getDistrict());
        project.setLongitude(payload.getLongitude());
        project.setLatitude(payload.getLatitude());
        project.setLeaderUserId(payload.getLeaderUserId());
        project.setLeaderName(payload.getLeaderName());
        project.setLeaderPhone(payload.getLeaderPhone());
        project.setDescription(payload.getDescription());
        project.setCreatorId(payload.getCreatorId());
        project.setCreatorDeptId(payload.getCreatorDeptId());
        return project;
    }

    /**
     * 判断项目当前状态是否仍允许编辑类操作。
     *
     * @param status 项目状态
     * @return 是否可编辑
     */
    private boolean isEditableStatus(Integer status) {
        return status == null || Objects.equals(status, 0) || Objects.equals(status, 3);
    }

    /**
     * 判断项目是否允许删除。
     * 管理员在可编辑状态基础上，额外允许删除已通过状态。
     *
     * @param status 项目状态
     * @param isAdmin 当前用户是否管理员
     * @return 是否允许删除
     */
    private boolean canDeleteStatus(Integer status, boolean isAdmin) {
        if (isEditableStatus(status)) {
            return true;
        }
        return isAdmin && Objects.equals(status, 2);
    }

    /**
     * 校验联系电话格式。
     * 允许为空，非空时需符合 7-20 位数字/短横线格式。
     *
     * @param phone 联系电话
     * @return 校验失败时返回错误提示，成功返回 null
     */
    private String validateLeaderPhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return null;
        }
        String normalizedPhone = phone.trim();
        if (!CONTACT_PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            return "联系电话格式不正确，请填写7到20位数字，可包含短横线";
        }
        return null;
    }

    /**
     * 获取当前登录人的统一访问上下文。
     *
     * @return 当前访问上下文
     */
    private UserAccessContext currentAccessContext() {
        return sysUserService.getAccessContext(StpUtil.getLoginIdAsLong());
    }
}
