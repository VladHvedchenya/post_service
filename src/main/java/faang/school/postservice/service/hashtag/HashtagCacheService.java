package faang.school.postservice.service.hashtag;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

@Service
public class HashtagCacheService {
    private final RedisTemplate<String, String> textRedisTemplate;
    private static final String POPULAR_HASHTAGS_KEY = "hashtags:popular";

    public HashtagCacheService(@Qualifier("textRedisTemplate") RedisTemplate<String, String> textRedisTemplate) {
        this.textRedisTemplate = textRedisTemplate;
    }

    public void incrementUsage(String hashtagName) {
        if (hashtagName == null) {
            return;
        }

        String cleanName = getClearHashtagName(hashtagName);
        textRedisTemplate.opsForZSet().incrementScore(POPULAR_HASHTAGS_KEY, cleanName, 1.0);
    }

    public void addOrUpdateScore(String hashtagName, double score) {
        if (hashtagName == null) {
            return;
        }

        String cleanName = getClearHashtagName(hashtagName);
        textRedisTemplate.opsForZSet().add(POPULAR_HASHTAGS_KEY, cleanName, score);
    }

    public Set<String> getTopHashtags(int limit) {
        Set<String> top = textRedisTemplate.opsForZSet().reverseRange(POPULAR_HASHTAGS_KEY, 0, limit - 1);
        return top != null ? top : Collections.emptySet();
    }

    public void clearCache() {
        textRedisTemplate.delete(POPULAR_HASHTAGS_KEY);
    }

    private String getClearHashtagName(String hashtag) {
        return hashtag.trim().toLowerCase().replace("#", "").trim();
    }
}