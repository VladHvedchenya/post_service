package faang.school.postservice.hashtag;

import faang.school.postservice.scheduler.HashtagCacheWarmingScheduler;
import faang.school.postservice.service.hashtag.HashtagService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HashtagCacheWarmingSchedulerTest {

    @Mock
    private HashtagService hashtagService;

    @InjectMocks
    private HashtagCacheWarmingScheduler scheduler;

    @Test
    @DisplayName("Successful cache warming")
    void shouldTriggerCacheWarming() {
        scheduler.warmPopularHashtagsCache();

        verify(hashtagService, times(1)).refreshPopularHashtagsCache();
    }
}