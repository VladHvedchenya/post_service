package faang.school.postservice.publisher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaPostCreatedEventTransportTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private KafkaPostCreatedEventTransport transport;

    @BeforeEach
    void setUp() {
        transport = new KafkaPostCreatedEventTransport(kafkaTemplate, "post_topic");
    }

    @Test
    void shouldSendKeyAndJsonOnConfiguredTopic() {
        String json = "{\"postId\":105,\"authorId\":42,\"projectId\":null}";
        when(kafkaTemplate.send("post_topic", "42", json))
                .thenReturn(CompletableFuture.completedFuture(null));

        transport.send("42", json);

        verify(kafkaTemplate).send("post_topic", "42", json);
    }

    @Test
    void shouldWaitForSendCompletion() throws Exception {
        CompletableFuture<SendResult<String, String>> sendResult = new CompletableFuture<>();
        CountDownLatch sendStarted = new CountDownLatch(1);
        when(kafkaTemplate.send("post_topic", "42", "{}")).thenAnswer(invocation -> {
            sendStarted.countDown();
            return sendResult;
        });
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            Future<?> sending = executor.submit(() -> transport.send("42", "{}"));
            assertTrue(sendStarted.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> sending.get(100, TimeUnit.MILLISECONDS));

            sendResult.complete(null);

            sending.get(5, TimeUnit.SECONDS);
        } finally {
            sendResult.complete(null);
            executor.shutdownNow();
        }
    }

    @Test
    void shouldPropagateAsynchronousSendFailure() {
        KafkaException failure = new KafkaException("Kafka unavailable");
        when(kafkaTemplate.send("post_topic", "42", "{}"))
                .thenReturn(CompletableFuture.failedFuture(failure));

        CompletionException exception = assertThrows(
                CompletionException.class,
                () -> transport.send("42", "{}")
        );

        assertSame(failure, exception.getCause());
    }

    @Test
    void shouldPropagateImmediateSendFailure() {
        KafkaException failure = new KafkaException("Producer unavailable");
        when(kafkaTemplate.send("post_topic", "42", "{}")).thenThrow(failure);

        KafkaException exception = assertThrows(
                KafkaException.class,
                () -> transport.send("42", "{}")
        );

        assertSame(failure, exception);
    }
}
