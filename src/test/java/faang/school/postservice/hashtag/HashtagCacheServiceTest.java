package faang.school.postservice.hashtag;

import faang.school.postservice.service.hashtag.HashtagCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HashtagCacheServiceTest {

    @Mock
    private RedisTemplate<String, String> hashtagRedisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private HashtagCacheService hashtagCacheService;

    private static final String KEY = "hashtags:popular";

    @Test
    @DisplayName("Successful incrementation")
    void shouldIncrementUsageWithCleanedName() {
        when(hashtagRedisTemplate.opsForZSet()).thenReturn(zSetOperations);

        hashtagCacheService.incrementUsage(" #Java ");

        verify(zSetOperations, times(1)).incrementScore(KEY, "java", 1.0);
    }

    @Test
    @DisplayName("Successful update and score insertion")
    void shouldAddOrUpdateScoreWithCleanedName() {
        when(hashtagRedisTemplate.opsForZSet()).thenReturn(zSetOperations);

        hashtagCacheService.addOrUpdateScore("  #Spring  ", 42.0);

        verify(zSetOperations, times(1)).add(KEY, "spring", 42.0);
    }

    @Test
    @DisplayName("Successful gettin top hashtags")
    void shouldReturnTopHashtagsFromCache() {
        Set<String> mockResult = Set.of("java", "spring");
        when(hashtagRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange(KEY, 0, 9)).thenReturn(mockResult);

        Set<String> result = hashtagCacheService.getTopHashtags(10);

        assertEquals(2, result.size());
        assertTrue(result.contains("java"));
        assertTrue(result.contains("spring"));
    }

    @Test
    @DisplayName("Get empty set when redis return null")
    void shouldReturnEmptySetWhenRedisReturnsNull() {
        when(hashtagRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange(KEY, 0, 4)).thenReturn(null);

        Set<String> result = hashtagCacheService.getTopHashtags(5);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Successful cache clearance")
    void shouldClearCache() {
        hashtagCacheService.clearCache();

        verify(hashtagRedisTemplate, times(1)).delete(KEY);
    }

    @Test
    @DisplayName("No increment when hashtag is null")
    void shouldDoNothingWhenIncrementUsageWithNull() {
        assertDoesNotThrow(() -> hashtagCacheService.incrementUsage(null));
        verify(hashtagRedisTemplate, never()).opsForZSet();
    }

    @Test
    @DisplayName("No add or update when hashtag is null")
    void shouldDoNothingWhenAddOrUpdateScoreWithNull() {
        assertDoesNotThrow(() -> hashtagCacheService.addOrUpdateScore(null, 10.0));
        verify(hashtagRedisTemplate, never()).opsForZSet();
    }
}