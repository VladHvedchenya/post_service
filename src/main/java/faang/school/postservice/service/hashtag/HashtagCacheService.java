package faang.school.postservice.service.hashtag;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HashtagCacheService {
    private final RedisTemplate<String, String> hashtagRedisTemplate;
    private static final String POPULAR_HASHTAGS_KEY = "hashtags:popular";

    public void incrementUsage(String hashtagName) {
        if (hashtagName == null) {
            return;
        }

        String cleanName = getClearHashtagName(hashtagName);
        hashtagRedisTemplate.opsForZSet().incrementScore(POPULAR_HASHTAGS_KEY, cleanName, 1.0);
    }

    public void addOrUpdateScore(String hashtagName, double score) {
        if (hashtagName == null) {
            return;
        }

        String cleanName = getClearHashtagName(hashtagName);
        hashtagRedisTemplate.opsForZSet().add(POPULAR_HASHTAGS_KEY, cleanName, score);
    }

    public Set<String> getTopHashtags(int limit) {
        Set<String> top = hashtagRedisTemplate.opsForZSet().reverseRange(POPULAR_HASHTAGS_KEY, 0, limit - 1);
        return top != null ? top : Collections.emptySet();
    }

    public void clearCache() {
        hashtagRedisTemplate.delete(POPULAR_HASHTAGS_KEY);
    }

    private String getClearHashtagName(String hashtag) {
        return hashtag.trim().toLowerCase().replace("#", "").trim();
    }
}