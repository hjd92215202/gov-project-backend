package com.gov.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Statement;
import java.util.Properties;

/**
 * MyBatis-Plus 基础配置。
 * 开启分页插件 + 慢 SQL 拦截器，防止单次大查询拖垮接口。
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 插件链。
     * 启用分页插件并约束单页最大条数，防止误查询超大结果集。
     *
     * @return MyBatis-Plus 拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.MARIADB);
        // 单页最大限制 50 条
        paginationInnerInterceptor.setMaxLimit(50L);
        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        return interceptor;
    }

    /**
     * 慢 SQL 拦截器。
     * 超过阈值的 SQL 会输出 warn 日志，便于定位性能瓶颈。
     *
     * @return 慢 SQL 拦截器
     */
    @Bean
    public SlowSqlInterceptor slowSqlInterceptor() {
        return new SlowSqlInterceptor();
    }

    @Intercepts({
        @Signature(type = StatementHandler.class, method = "query", args = {Statement.class, ResultHandler.class}),
        @Signature(type = StatementHandler.class, method = "update", args = {Statement.class})
    })
    public static class SlowSqlInterceptor implements Interceptor {

        private static final Logger log = LoggerFactory.getLogger("com.gov.perf");

        @Value("${gov.logging.slow-sql-ms:500}")
        private long slowSqlThresholdMs;

        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            long start = System.currentTimeMillis();
            try {
                return invocation.proceed();
            } finally {
                long cost = System.currentTimeMillis() - start;
                if (cost >= slowSqlThresholdMs) {
                    StatementHandler handler = (StatementHandler) invocation.getTarget();
                    BoundSql boundSql = handler.getBoundSql();
                    String sql = boundSql.getSql().replaceAll("\\s+", " ").trim();
                    log.warn("慢SQL告警 costMs={} sql={}", cost, sql.length() > 500 ? sql.substring(0, 500) + "..." : sql);
                }
            }
        }

        @Override
        public Object plugin(Object target) {
            return Plugin.wrap(target, this);
        }

        @Override
        public void setProperties(Properties properties) {
        }
    }
}
