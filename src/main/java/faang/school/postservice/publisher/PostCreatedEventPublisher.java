package faang.school.postservice.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.dto.event.PostCreatedEvent;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

@Component
public class PostCreatedEventPublisher {
    private final Validator validator;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final ChannelTopic channelTopic;

    public PostCreatedEventPublisher(
            Validator validator,
            ObjectMapper objectMapper,
            @Qualifier("textRedisTemplate") RedisTemplate<String, String> redisTemplate,
            @Qualifier("postCreatedTopic") ChannelTopic channelTopic
    ){
        this.validator = validator;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.channelTopic = channelTopic;
    }

    public void publish(PostCreatedEvent event){
        validateEvent(event);
        String json = serializeEvent(event);
        redisTemplate.convertAndSend(channelTopic.getTopic(), json);
    }

    private void validateEvent(PostCreatedEvent event){
        Objects.requireNonNull(event);
        Set<ConstraintViolation<PostCreatedEvent>> violations = validator.validate(event);

        if(!violations.isEmpty()){
            throw new ConstraintViolationException(violations);
        }
    }

    private String serializeEvent(PostCreatedEvent event){
        try{
            return objectMapper.writeValueAsString(event);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize PostCreatedEvent for postId=" + event.postId(),
                    exception);
        }
    }
}
