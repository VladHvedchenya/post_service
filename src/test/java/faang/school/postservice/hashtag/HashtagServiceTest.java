package faang.school.postservice.hashtag;

import faang.school.postservice.model.Hashtag;
import faang.school.postservice.repository.HashtagRepository;
import faang.school.postservice.service.hashtag.HashtagCacheService;
import faang.school.postservice.service.hashtag.HashtagService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HashtagServiceTest {

    @Mock
    private HashtagRepository hashtagRepository;

    @Mock
    private HashtagCacheService hashtagCacheService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query nativeQuery;

    @InjectMocks
    private HashtagService hashtagService;

    @Test
    @DisplayName("Successful hashtags parsing from text and processing race condition with ")
    void shouldParseAndBindHashtagsWithRaceConditionHandling() {
        Long postId = 42L;
        String text = "Пишу код на #Java в экосистеме #CorporationX";

        Hashtag existingHashtag = Hashtag.builder().id(1L).name("java").build();
        Hashtag racedHashtag = Hashtag.builder().id(2L).name("corporationx").build();

        when(hashtagRepository.findByNameIgnoreCase("java")).thenReturn(Optional.of(existingHashtag));
        when(hashtagRepository.findByNameIgnoreCase("corporationx"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(racedHashtag));

        when(hashtagRepository.saveAndFlush(any(Hashtag.class))).thenThrow(new RuntimeException("DB Unique Constraint Violated"));

        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
        when(nativeQuery.executeUpdate()).thenReturn(1);

        hashtagService.parseAndBindHashtags(postId, text);

        verify(hashtagRepository, times(3)).findByNameIgnoreCase(anyString());
        verify(hashtagCacheService, times(1)).incrementUsage("java");
        verify(hashtagCacheService, times(1)).incrementUsage("corporationx");
        verify(nativeQuery, times(2)).executeUpdate();
    }

    @Test
    @DisplayName("Successful refresh of popular hashtags")
    void shouldRefreshPopularHashtagsCacheSuccessfully() {
        List<Object[]> mockDbResult = List.of(
                new Object[]{"java", 150L},
                new Object[]{"spring", 90L}
        );
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.getResultList()).thenReturn(mockDbResult);

        hashtagService.refreshPopularHashtagsCache();

        verify(hashtagCacheService, times(1)).clearCache();
        verify(hashtagCacheService).addOrUpdateScore("java", 150.0);
        verify(hashtagCacheService).addOrUpdateScore("spring", 90.0);
    }

    @Test
    @DisplayName("Abort refreshing when DB is empty")
    void shouldAbortRefreshingWhenDbIsEmpty() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.getResultList()).thenReturn(Collections.emptyList());

        hashtagService.refreshPopularHashtagsCache();

        verify(hashtagCacheService, never()).clearCache();
        verify(hashtagCacheService, never()).addOrUpdateScore(anyString(), anyDouble());
    }

    @Test
    @DisplayName("Check for no exception during caching")
    void shouldHandleExceptionDuringCacheRefresh() {
        when(entityManager.createNativeQuery(anyString())).thenThrow(new RuntimeException("PostgreSQL Connection Lost"));

        assertDoesNotThrow(() -> hashtagService.refreshPopularHashtagsCache());
        verify(hashtagCacheService, never()).clearCache();
    }

    @Test
    @DisplayName("Successful return of top hashtags from Redis Cache")
    void shouldReturnTopHashtagsDirectlyFromCache() {
        int limit = 10;
        Set<String> mockCache = Set.of("java", "spring");
        when(hashtagCacheService.getTopHashtags(limit)).thenReturn(mockCache);

        Set<String> result = hashtagService.getTopHashtagsWithFallback(limit);

        org.junit.jupiter.api.Assertions.assertEquals(mockCache, result);
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    @DisplayName("Successful Redis Cache warming")
    void shouldWarmCacheWhenCacheIsEmptyAndLockAcquired() {
        int limit = 5;

        List<Object[]> mockDbResult = new java.util.ArrayList<>();
        mockDbResult.add(new Object[]{"java", 100L});

        when(hashtagCacheService.getTopHashtags(limit))
                .thenReturn(Collections.emptySet())
                .thenReturn(Set.of("java"));

        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.getResultList()).thenReturn(mockDbResult);

        Set<String> result = hashtagService.getTopHashtagsWithFallback(limit);

        org.junit.jupiter.api.Assertions.assertFalse(result.isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(result.contains("java"));
        verify(hashtagCacheService, times(1)).clearCache();
    }

    @Test
    @DisplayName("Check correct parsing with empty or null")
    void shouldReturnEarlyWhenTextIsNullOrEmptyInParser() {
        hashtagService.parseAndBindHashtags(1L, "");
        hashtagService.parseAndBindHashtags(1L, null);

        verify(hashtagRepository, never()).findByNameIgnoreCase(anyString());
    }
}