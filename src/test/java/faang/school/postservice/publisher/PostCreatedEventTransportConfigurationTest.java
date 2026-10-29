package faang.school.postservice.publisher;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PostCreatedEventTransportConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            .withPropertyValues("notifications.kafka.topic=post_topic");

    @Test
    void shouldSelectOnlyRedisByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(PostCreatedEventTransport.class);
            assertThat(context).hasSingleBean(RedisPostCreatedEventTransport.class);
            assertThat(context).doesNotHaveBean(KafkaPostCreatedEventTransport.class);
        });
    }

    @Test
    void shouldSelectOnlyRedisWhenConfigured() {
        contextRunner.withPropertyValues("notifications.transport=redis").run(context -> {
            assertThat(context).hasSingleBean(PostCreatedEventTransport.class);
            assertThat(context).hasSingleBean(RedisPostCreatedEventTransport.class);
            assertThat(context).doesNotHaveBean(KafkaPostCreatedEventTransport.class);
        });
    }

    @Test
    void shouldSelectOnlyKafkaWhenConfigured() {
        contextRunner.withPropertyValues("notifications.transport=kafka").run(context -> {
            assertThat(context).hasSingleBean(PostCreatedEventTransport.class);
            assertThat(context).hasSingleBean(KafkaPostCreatedEventTransport.class);
            assertThat(context).doesNotHaveBean(RedisPostCreatedEventTransport.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({RedisPostCreatedEventTransport.class, KafkaPostCreatedEventTransport.class})
    static class TestConfig {

        @Bean
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> textRedisTemplate() {
            return mock(RedisTemplate.class);
        }

        @Bean
        ChannelTopic postCreatedTopic() {
            return new ChannelTopic("post.created");
        }

        @Bean
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate() {
            return mock(KafkaTemplate.class);
        }
    }
}
