package com.gov.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 职责：为脱离主请求链路的异步任务提供统一线程池配置。
 * 为什么存在：审计日志、观测补充等任务不应阻塞用户点击后的主响应时间，
 * 需要通过有界线程池把耗时写库动作平滑移出同步请求链路。
 */
@Configuration
@EnableAsync
public class AsyncTaskConfig {

    /**
     * 审计日志异步线程池。
     * 100人并发场景下调大核心线程数，避免队列积压后 CallerRunsPolicy 拖慢主线程。
     * 使用有界队列并在极端拥塞时退回调用线程，避免无限堆积导致内存风险。
     *
     * @return 审计日志线程池执行器
     */
    @Bean("auditLogExecutor")
    public Executor auditLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(5000);
        executor.setThreadNamePrefix("audit-log-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
