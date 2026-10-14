package faang.school.postservice.scheduler;

import faang.school.postservice.service.post.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScheduledPostPublisherTest {

    @Mock
    private PostService postService;

    @InjectMocks
    private ScheduledPostPublisher scheduledPostPublisher;

    @Test
    @DisplayName("Should delegate scheduled posts publishing to post service")
    void shouldPublishScheduledPosts() {
        scheduledPostPublisher.publishScheduledPosts();

        verify(postService).publishScheduledPosts();
    }
}
