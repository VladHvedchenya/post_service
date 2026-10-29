package faang.school.postservice.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.dto.event.PostCreatedEvent;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

@Component
public class PostCreatedEventPublisher {
    private final Validator validator;
    private final ObjectMapper objectMapper;
    private final PostCreatedEventTransport transport;

    public PostCreatedEventPublisher(
            Validator validator,
            ObjectMapper objectMapper,
            PostCreatedEventTransport transport
    ) {
        this.validator = validator;
        this.objectMapper = objectMapper;
        this.transport = transport;
    }

    public void publish(PostCreatedEvent event) {
        validateEvent(event);
        String json = serializeEvent(event);
        transport.send(event.authorId().toString(), json);
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
