package com.inventalert.inventoryService.config;

import com.inventalert.inventoryService.multicompany.CompanyContext;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("inventory-async-");
        executor.setTaskDecorator(runnable -> {
            String schemaName = CompanyContext.get();
            return () -> {
                try {
                    if (schemaName != null) {
                        CompanyContext.setRaw(schemaName);
                    }
                    runnable.run();
                } finally {
                    CompanyContext.clear();
                }
            };
        });
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                LoggerFactory.getLogger(AsyncConfig.class)
                        .error("Uncaught async exception in {}: {}", method.getName(), ex.getMessage(), ex);
    }
}
