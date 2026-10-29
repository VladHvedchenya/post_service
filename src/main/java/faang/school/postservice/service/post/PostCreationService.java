package faang.school.postservice.service.post;

import faang.school.postservice.dto.post.CreatePostRequest;
import faang.school.postservice.dto.event.PostCreatedEvent;
import faang.school.postservice.dto.post.PostResponse;
import faang.school.postservice.exception.DataValidationException;
import faang.school.postservice.model.Post;
import faang.school.postservice.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostCreationService {
    private final PostRepository postRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public PostResponse create(long authorId, CreatePostRequest request) {
        if (authorId <= 0) {
            throw new DataValidationException("Author id must be positive");
        }

        Post post = Post.builder()
                .content(request.content())
                .authorId(authorId)
                .projectId(request.projectId())
                .build();
        Post savedPost = postRepository.save(post);

        applicationEventPublisher.publishEvent(new PostCreatedEvent(
                savedPost.getId(), savedPost.getAuthorId(), savedPost.getProjectId()));

        return new PostResponse(
                savedPost.getId(),
                savedPost.getContent(),
                savedPost.getAuthorId(),
                savedPost.getProjectId(),
                savedPost.isPublished());
    }
}
