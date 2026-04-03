package com.gov.common.controller;

import com.gov.common.result.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 职责：提供轻量健康检查接口，供负载均衡、容器探针、运维巡检使用。
 * 为什么存在：没有健康检查接口，容器/进程守护无法感知服务是否真正可用，
 * 重启后也无法判断服务是否已就绪。
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    /**
     * 轻量存活探针，只要进程在就返回 UP。
     * 适合 liveness probe。
     *
     * @return 存活状态
     */
    @GetMapping("/live")
    public R<String> live() {
        return R.ok("UP");
    }

    /**
     * 就绪探针，同时检查数据库连通性。
     * 适合 readiness probe 和负载均衡健康检查。
     *
     * @return 就绪状态与各依赖状态
     */
    @GetMapping("/ready")
    public R<Map<String, String>> ready() {
        Map<String, String> status = new LinkedHashMap<>();
        status.put("app", "UP");

        String dbStatus = checkDatabase();
        status.put("db", dbStatus);

        boolean allUp = status.values().stream().allMatch("UP"::equals);
        if (!allUp) {
            return R.fail(503, "服务未就绪");
        }
        return R.ok(status, "就绪");
    }

    private String checkDatabase() {
        if (jdbcTemplate == null) {
            return "UNKNOWN";
        }
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "UP";
        } catch (Exception e) {
            return "DOWN";
        }
    }
}
