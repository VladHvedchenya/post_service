package faang.school.postservice.publisher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.config.redis.RedisConfig;
import faang.school.postservice.dto.event.PostCreatedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.SubscriptionListener;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringJUnitConfig(PostCreatedEventPublisherIntegrationTest.TestConfig.class)
@TestPropertySource(properties = "notifications.channels.post-created=notification-test.post.created")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PostCreatedEventPublisherIntegrationTest {
    @Container
    private static final GenericContainer<?> redisContainer = new GenericContainer<>(
            DockerImageName.parse("redis:7.2-alpine")
    ).withExposedPorts(6379);

    private final PostCreatedEventPublisher publisher;
    private final RedisConnectionFactory connectionFactory;
    private final ObjectMapper objectMapper;
    private final ChannelTopic channelTopic;
    private final List<RedisMessageListenerContainer> listenerContainers = new ArrayList<>();

    @Autowired
    PostCreatedEventPublisherIntegrationTest(
            PostCreatedEventPublisher publisher,
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper,
            @Qualifier("postCreatedTopic") ChannelTopic channelTopic
    ) {
        this.publisher = publisher;
        this.connectionFactory = connectionFactory;
        this.objectMapper = objectMapper;
        this.channelTopic = channelTopic;
    }

    @AfterEach
    void closeSubscriptions() throws Exception {
        for (RedisMessageListenerContainer listenerContainer : listenerContainers) {
            listenerContainer.destroy();
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {17L})
    void shouldDeliverEventThroughRedisOnConfiguredChannel(Long projectId) throws Exception {
        CapturingListener subscriber = subscribe();
        PostCreatedEvent event = new PostCreatedEvent(105L, 42L, projectId);

        publisher.publish(event);

        Message message = subscriber.receivedMessage.get(10, TimeUnit.SECONDS);
        assertReceivedEvent(event, message);
    }

    @Test
    void shouldDeliverEventToEverySubscriber() throws Exception {
        CapturingListener firstSubscriber = subscribe();
        CapturingListener secondSubscriber = subscribe();
        PostCreatedEvent event = new PostCreatedEvent(105L, 42L, 17L);

        publisher.publish(event);

        assertReceivedEvent(event, firstSubscriber.receivedMessage.get(10, TimeUnit.SECONDS));
        assertReceivedEvent(event, secondSubscriber.receivedMessage.get(10, TimeUnit.SECONDS));
    }

    private CapturingListener subscribe() throws Exception {
        CapturingListener listener = new CapturingListener();
        RedisMessageListenerContainer listenerContainer = new RedisMessageListenerContainer();
        listenerContainers.add(listenerContainer);
        listenerContainer.setConnectionFactory(connectionFactory);
        listenerContainer.addMessageListener(listener, channelTopic);
        listenerContainer.afterPropertiesSet();
        listenerContainer.start();
        listener.subscriptionConfirmed.get(10, TimeUnit.SECONDS);
        return listener;
    }

    private void assertReceivedEvent(PostCreatedEvent expected, Message message) throws Exception {
        assertEquals("notification-test.post.created", new String(message.getChannel(), StandardCharsets.UTF_8));
        JsonNode json = objectMapper.readTree(message.getBody());
        assertEquals(3, json.size());
        assertEquals(expected.postId().longValue(), json.get("postId").longValue());
        assertEquals(expected.authorId().longValue(), json.get("authorId").longValue());
        if (expected.projectId() == null) {
            assertTrue(json.get("projectId").isNull());
        } else {
            assertEquals(expected.projectId().longValue(), json.get("projectId").longValue());
        }
        assertEquals(expected, objectMapper.readValue(message.getBody(), PostCreatedEvent.class));
    }

    private static class CapturingListener implements MessageListener, SubscriptionListener {
        private final CompletableFuture<Void> subscriptionConfirmed = new CompletableFuture<>();
        private final CompletableFuture<Message> receivedMessage = new CompletableFuture<>();

        @Override
        public void onMessage(Message message, byte[] pattern) {
            receivedMessage.complete(message);
        }

        @Override
        public void onChannelSubscribed(byte[] channel, long count) {
            subscriptionConfirmed.complete(null);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import({RedisConfig.class, PostCreatedEventPublisher.class})
    static class TestConfig {
        @Bean
        LettuceConnectionFactory redisConnectionFactory() {
            return new LettuceConnectionFactory(redisContainer.getHost(), redisContainer.getMappedPort(6379));
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        LocalValidatorFactoryBean validator() {
            return new LocalValidatorFactoryBean();
        }
    }
}
