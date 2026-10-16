package faang.school.postservice.post;

import faang.school.postservice.dto.post.CreatePostRequest;
import faang.school.postservice.dto.post.PostResponse;
import faang.school.postservice.exception.DataValidationException;
import faang.school.postservice.model.Post;
import faang.school.postservice.repository.PostRepository;
import faang.school.postservice.service.post.PostCreationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostCreationServiceTest {
    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostCreationService postCreationService;

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = 17L)
    void shouldCreateDraftForCurrentAuthor(Long projectId) {
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            post.setId(105L);
            return post;
        });

        PostResponse response = postCreationService.create(1L, new CreatePostRequest("New post", projectId));

        assertThat(response).isEqualTo(new PostResponse(105L, "New post", 1L, projectId, false));
        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(postCaptor.capture());
        assertThat(postCaptor.getValue().isPublished()).isFalse();
        assertThat(postCaptor.getValue().isDeleted()).isFalse();
        assertThat(postCaptor.getValue().getScheduledAt()).isNull();
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void shouldRejectInvalidAuthor(long authorId) {
        assertThatThrownBy(() -> postCreationService.create(authorId, new CreatePostRequest("New post", null)))
                .isInstanceOf(DataValidationException.class);
        verifyNoInteractions(postRepository);
    }

    @Test
    void shouldPropagatePersistenceFailure() {
        RuntimeException failure = new RuntimeException("Database unavailable");
        when(postRepository.save(any(Post.class))).thenThrow(failure);

        assertThatThrownBy(() -> postCreationService.create(1L, new CreatePostRequest("New post", null)))
                .isSameAs(failure);
    }
}
