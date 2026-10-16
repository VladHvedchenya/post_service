package faang.school.postservice.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.dto.event.PostCreatedEvent;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostCreatedEventPublisherTest {
    private static ValidatorFactory validatorFactory;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private PostCreatedEventPublisher publisher;

    @BeforeAll
    static void createValidatorFactory() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void closeValidatorFactory() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
    }

    @BeforeEach
    void setUp() {
        publisher = new PostCreatedEventPublisher(
                validatorFactory.getValidator(),
                objectMapper,
                redisTemplate,
                new ChannelTopic("post.created")
        );
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {17L})
    void shouldPublishJsonWithAllEventFields(Long projectId) throws Exception {
        PostCreatedEvent event = new PostCreatedEvent(105L, 42L, projectId);

        publisher.publish(event);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(eq("post.created"), jsonCaptor.capture());
        assertEquals(event, objectMapper.readValue(jsonCaptor.getValue(), PostCreatedEvent.class));
        assertEquals(3, objectMapper.readTree(jsonCaptor.getValue()).size());
    }

    @Test
    void shouldRejectNullEventBeforeSerializationOrPublishing() {
        assertThrows(NullPointerException.class, () -> publisher.publish(null));

        verifyNoInteractions(objectMapper, redisTemplate);
    }

    @ParameterizedTest
    @MethodSource("invalidEvents")
    void shouldRejectMissingRequiredFieldsBeforeSerializationOrPublishing(
            PostCreatedEvent event,
            Set<String> expectedInvalidFields
    ) {
        ConstraintViolationException exception = assertThrows(
                ConstraintViolationException.class,
                () -> publisher.publish(event)
        );

        Set<String> invalidFields = exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
        assertEquals(expectedInvalidFields, invalidFields);
        verifyNoInteractions(objectMapper, redisTemplate);
    }

    @Test
    void shouldPreserveSerializationFailureAndNotPublish() throws Exception {
        PostCreatedEvent event = new PostCreatedEvent(105L, 42L, null);
        JsonProcessingException serializationFailure = new JsonProcessingException("Serialization failed") {
        };
        doThrow(serializationFailure).when(objectMapper).writeValueAsString(event);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> publisher.publish(event)
        );

        assertSame(serializationFailure, exception.getCause());
        assertTrue(exception.getMessage().contains("PostCreatedEvent"));
        assertTrue(exception.getMessage().contains("postId=105"));
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void shouldPropagateRedisFailure() {
        PostCreatedEvent event = new PostCreatedEvent(105L, 42L, null);
        RedisConnectionFailureException redisFailure = new RedisConnectionFailureException("Redis unavailable");
        when(redisTemplate.convertAndSend(eq("post.created"), anyString())).thenThrow(redisFailure);

        RedisConnectionFailureException exception = assertThrows(
                RedisConnectionFailureException.class,
                () -> publisher.publish(event)
        );

        assertSame(redisFailure, exception);
    }

    @Test
    void shouldAllowPublishingWithoutSubscribers() {
        PostCreatedEvent event = new PostCreatedEvent(105L, 42L, null);
        when(redisTemplate.convertAndSend(eq("post.created"), anyString())).thenReturn(0L);

        assertDoesNotThrow(() -> publisher.publish(event));

        verify(redisTemplate).convertAndSend(eq("post.created"), anyString());
    }

    private static Stream<Arguments> invalidEvents() {
        return Stream.of(
                Arguments.of(new PostCreatedEvent(null, 42L, null), Set.of("postId")),
                Arguments.of(new PostCreatedEvent(105L, null, null), Set.of("authorId")),
                Arguments.of(new PostCreatedEvent(null, null, null), Set.of("postId", "authorId"))
        );
    }
}
