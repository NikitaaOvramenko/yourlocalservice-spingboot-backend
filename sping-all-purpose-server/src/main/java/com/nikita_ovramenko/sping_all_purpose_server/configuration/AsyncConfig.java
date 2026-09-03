package com.nikita_ovramenko.sping_all_purpose_server.configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Background execution for outbound mail. */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * A dedicated, deliberately small and bounded pool rather than Boot's default
     * task executor, whose queue is effectively unbounded -- under a burst of
     * submissions that grows the heap instead of pushing back.
     *
     * <p>Note how ThreadPoolExecutor grows: threads beyond the core size are only
     * created once the queue is full, so in practice this runs on 2 threads and only
     * reaches 4 if 100 emails are already waiting.
     */
    @Bean("emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("email-");

        // When the queue is full, run the task on the calling thread instead of
        // discarding it. That degrades to the old synchronous behaviour under extreme
        // load, which is the right trade: a slow submission beats a silently lost one.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Let in-flight emails finish on shutdown rather than dropping them mid-send.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }
}
