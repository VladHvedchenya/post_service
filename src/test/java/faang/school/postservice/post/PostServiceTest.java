package faang.school.postservice.post;

import faang.school.postservice.model.Post;
import faang.school.postservice.repository.PostRepository;
import faang.school.postservice.service.post.PostService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    private ExecutorService executorService;

    @AfterEach
    void tearDown() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @Test
    @DisplayName("Should not save posts when there are no ready posts")
    void shouldNotSavePostsWhenThereAreNoReadyPosts() {
        executorService = Executors.newSingleThreadExecutor();
        PostService postService = new PostService(postRepository, executorService, 1000);

        when(postRepository.findReadyToPublish()).thenReturn(List.of());

        postService.publishScheduledPosts();

        verify(postRepository).findReadyToPublish();
        verify(postRepository, never()).saveAll(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Should publish ready posts and save them")
    void shouldPublishReadyPostsAndSaveThem() {
        executorService = Executors.newSingleThreadExecutor();
        PostService postService = new PostService(postRepository, executorService, 1000);
        List<Post> posts = List.of(createScheduledPost(), createScheduledPost());

        when(postRepository.findReadyToPublish()).thenReturn(posts);

        postService.publishScheduledPosts();

        posts.forEach(post -> {
            assertTrue(post.isPublished());
            assertNotNull(post.getPublishedAt());
        });
        verify(postRepository).saveAll(posts);
    }

    @Test
    @DisplayName("Should split ready posts into batches")
    @SuppressWarnings("unchecked")
    void shouldSplitReadyPostsIntoBatches() {
        executorService = Executors.newSingleThreadExecutor();
        PostService postService = new PostService(postRepository, executorService, 2);
        List<Post> posts = List.of(
                createScheduledPost(),
                createScheduledPost(),
                createScheduledPost(),
                createScheduledPost(),
                createScheduledPost()
        );

        when(postRepository.findReadyToPublish()).thenReturn(posts);

        postService.publishScheduledPosts();

        ArgumentCaptor<Iterable<Post>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(postRepository, org.mockito.Mockito.times(3)).saveAll(captor.capture());

        List<Integer> batchSizes = captor.getAllValues().stream()
                .map(this::toList)
                .map(List::size)
                .sorted(Comparator.naturalOrder())
                .toList();

        assertTrue(posts.stream().allMatch(Post::isPublished));
        assertTrue(posts.stream().allMatch(post -> post.getPublishedAt() != null));
        org.junit.jupiter.api.Assertions.assertEquals(List.of(1, 2, 2), batchSizes);
    }

    private Post createScheduledPost() {
        return Post.builder()
                .published(false)
                .deleted(false)
                .scheduledAt(LocalDateTime.now().minusMinutes(1))
                .build();
    }

    private List<Post> toList(Iterable<Post> posts) {
        List<Post> result = new ArrayList<>();
        posts.forEach(result::add);
        return result;
    }
}
