package faang.school.postservice.service.hashtag;

import faang.school.postservice.model.Hashtag;
import faang.school.postservice.repository.HashtagRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class HashtagService {
    private final HashtagRepository hashtagRepository;
    private final HashtagCacheService hashtagCacheService;
    private final EntityManager entityManager;
    private final ReentrantLock lock = new ReentrantLock();

    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#\\w+");

    @Transactional
    public void parseAndBindHashtags(Long postId, String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        Set<String> hashtagNames = extractHashtags(text);

        for (String name : hashtagNames) {
            String cleanName = name.toLowerCase();

            Hashtag hashtag = hashtagRepository.findByNameIgnoreCase(cleanName)
                    .orElseGet(() -> {
                        Hashtag newHashtag = Hashtag.builder()
                                .name(cleanName)
                                .createdAt(LocalDateTime.now())
                                .build();
                        try {
                            return hashtagRepository.saveAndFlush(newHashtag);
                        } catch (Exception e) {
                            log.warn("Hashtag concurrent insertion race condition caught for name: {}", cleanName);
                            return hashtagRepository.findByNameIgnoreCase(cleanName)
                                    .orElseThrow(() -> new IllegalStateException("Failed to resolve hashtag race condition"));
                        }
                    });

            entityManager.createNativeQuery("""
                            INSERT INTO post_hashtag (post_id, hashtag_id) 
                            VALUES (:postId, :hashtagId) 
                            ON CONFLICT DO NOTHING
                            """)
                    .setParameter("postId", postId)
                    .setParameter("hashtagId", hashtag.getId())
                    .executeUpdate();

            hashtagCacheService.incrementUsage(cleanName);
        }
    }

    public Set<String> getTopHashtagsWithFallback(int limit) {
        Set<String> top = hashtagCacheService.getTopHashtags(limit);

        if (!top.isEmpty()) {
            return top;
        }

        if (lock.tryLock()) {
            try {
                log.warn("Cache miss detected for popular hashtags. Warming cache synchronously from DB...");
                refreshPopularHashtagsCache();
                top = hashtagCacheService.getTopHashtags(limit);
            } finally {
                lock.unlock();
            }
        } else {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            top = hashtagCacheService.getTopHashtags(limit);
        }

        return top;
    }

    @Transactional(readOnly = true)
    public void refreshPopularHashtagsCache() {
        log.info("Executing popular hashtags cache synchronization with Database...");
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> popularHashtags = entityManager.createNativeQuery("""
                    SELECT h.name, COUNT(ph.post_id) as usage_count
                    FROM hashtag h
                    JOIN post_hashtag ph ON h.id = ph.hashtag_id
                    GROUP BY h.id, h.name
                    ORDER BY usage_count DESC
                    LIMIT 100
                    """).getResultList();

            if (popularHashtags.isEmpty()) {
                log.warn("No hashtags found in database to warm cache.");
                return;
            }

            hashtagCacheService.clearCache();

            for (Object[] row : popularHashtags) {
                String name = (String) row[0];
                Long count = ((Number) row[1]).longValue();
                hashtagCacheService.addOrUpdateScore(name, count.doubleValue());
            }
            log.info("Successfully updated {} popular hashtags in Redis ZSET.", popularHashtags.size());
        } catch (Exception e) {
            log.error("Failed to refresh popular hashtags cache from database", e);
        }
    }

    private Set<String> extractHashtags(String text) {
        Set<String> hashtags = new HashSet<>();
        Matcher matcher = HASHTAG_PATTERN.matcher(text);

        while (matcher.find()) {
            hashtags.add(matcher.group().replace("#", ""));
        }

        return hashtags;
    }
}