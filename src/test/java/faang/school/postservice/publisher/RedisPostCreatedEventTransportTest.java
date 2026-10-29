package faang.school.postservice.publisher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisPostCreatedEventTransportTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    private RedisPostCreatedEventTransport transport;

    @BeforeEach
    void setUp() {
        transport = new RedisPostCreatedEventTransport(redisTemplate, new ChannelTopic("post.created"));
    }

    @Test
    void shouldSendJsonOnConfiguredChannel() {
        String json = "{\"postId\":105,\"authorId\":42,\"projectId\":null}";

        transport.send("42", json);

        verify(redisTemplate).convertAndSend("post.created", json);
    }

    @Test
    void shouldPropagateRedisFailure() {
        RedisConnectionFailureException failure = new RedisConnectionFailureException("Redis unavailable");
        when(redisTemplate.convertAndSend("post.created", "{}")).thenThrow(failure);

        RedisConnectionFailureException exception = assertThrows(
                RedisConnectionFailureException.class,
                () -> transport.send("42", "{}")
        );

        assertSame(failure, exception);
    }

    @Test
    void shouldAllowSendingWithoutSubscribers() {
        when(redisTemplate.convertAndSend("post.created", "{}")).thenReturn(0L);

        assertDoesNotThrow(() -> transport.send("42", "{}"));

        verify(redisTemplate).convertAndSend("post.created", "{}");
    }
}
