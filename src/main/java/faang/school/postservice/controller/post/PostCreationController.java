package faang.school.postservice.controller.post;

import faang.school.postservice.dto.post.CreatePostRequest;
import faang.school.postservice.dto.post.PostResponse;
import faang.school.postservice.service.post.PostCreationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
public class PostCreationController {
    private final PostCreationService postCreationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(
            @RequestHeader("x-user-id") long authorId,
            @Valid @RequestBody CreatePostRequest request
    ) {
        return postCreationService.create(authorId, request);
    }
}
