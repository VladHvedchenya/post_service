package faang.school.postservice.scheduler;

import faang.school.postservice.service.hashtag.HashtagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HashtagCacheWarmingScheduler {
    private final HashtagService hashtagService;

    @Scheduled(cron = "0 */10 * * * *")
    public void warmPopularHashtagsCache() {
        log.info("Scheduled task triggered for popular hashtags cache warming.");
        hashtagService.refreshPopularHashtagsCache();
    }
}