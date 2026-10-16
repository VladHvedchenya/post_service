package faang.school.postservice.listener;

import faang.school.postservice.dto.event.PostCreatedEvent;
import faang.school.postservice.publisher.PostCreatedEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostCreatedNotificationListener {
    private final PostCreatedEventPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostCreated(PostCreatedEvent event) {
        try {
            publisher.publish(event);
        } catch (RuntimeException exception) {
            log.error("Post committed but notification event could not be published: postId={}, authorId={}",
                    event.postId(), event.authorId(), exception);
        }
    }
}
