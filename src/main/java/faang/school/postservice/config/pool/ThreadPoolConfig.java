package faang.school.postservice.config.pool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ThreadPoolConfig {

    @Bean(name = "postPublisherExecutorService", destroyMethod = "shutdown")
    public ExecutorService postPublisherExecutorService(
            @Value("${post-publisher.thread-pool-size}") int threadPoolSize
    ) {
        return Executors.newFixedThreadPool(threadPoolSize);
    }
}