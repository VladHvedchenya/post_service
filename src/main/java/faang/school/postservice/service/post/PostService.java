package faang.school.postservice.service.post;

import faang.school.postservice.model.Post;
import faang.school.postservice.repository.PostRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final ExecutorService executorService;
    private final int batchSize;

    public PostService(
            PostRepository postRepository,
            @Qualifier("postPublisherExecutorService") ExecutorService executorService,
            @Value("${post-publisher.batch-size}") int batchSize
    ) {
        this.postRepository = postRepository;
        this.executorService = executorService;
        this.batchSize = batchSize;
    }

    public void publishScheduledPosts() {
        List<Post> posts = postRepository.findReadyToPublish();

        if (posts.isEmpty()) {
            return;
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < posts.size(); i += batchSize) {
            int toIndex = Math.min(i + batchSize, posts.size());
            List<Post> batch = new ArrayList<>(posts.subList(i, toIndex));
            CompletableFuture<Void> future = CompletableFuture.runAsync(
                    () -> publishBatch(batch),
                    executorService
            );
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private void publishBatch(List<Post> posts) {
        LocalDateTime time = LocalDateTime.now();

        for (var post : posts) {
            post.setPublished(true);
            post.setPublishedAt(time);
        }

        postRepository.saveAll(posts);
    }
}
