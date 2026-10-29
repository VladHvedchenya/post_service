package faang.school.postservice.publisher;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "notifications",
        name = "transport",
        havingValue = "redis",
        matchIfMissing = true
)
public class RedisPostCreatedEventTransport implements PostCreatedEventTransport {

    private final RedisTemplate<String, String> redisTemplate;
    private final ChannelTopic channelTopic;

    public RedisPostCreatedEventTransport(
            @Qualifier("textRedisTemplate") RedisTemplate<String, String> redisTemplate,
            @Qualifier("postCreatedTopic") ChannelTopic channelTopic
    ) {
        this.redisTemplate = redisTemplate;
        this.channelTopic = channelTopic;
    }

    @Override
    public void send(String key, String json) {
        redisTemplate.convertAndSend(channelTopic.getTopic(), json);
    }
}