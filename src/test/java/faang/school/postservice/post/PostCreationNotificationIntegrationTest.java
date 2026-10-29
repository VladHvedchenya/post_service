package faang.school.postservice.post;

import faang.school.postservice.dto.event.PostCreatedEvent;
import faang.school.postservice.dto.post.CreatePostRequest;
import faang.school.postservice.dto.post.PostResponse;
import faang.school.postservice.listener.PostCreatedNotificationListener;
import faang.school.postservice.model.Post;
import faang.school.postservice.publisher.PostCreatedEventPublisher;
import faang.school.postservice.repository.PostRepository;
import faang.school.postservice.service.post.PostCreationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@Testcontainers
@SpringBootTest(
        classes = PostCreationNotificationIntegrationTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.jpa.hibernate.ddl-auto=create-drop"
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PostCreationNotificationIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.6");

    @Autowired
    private PostCreationService postCreationService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostCreatedEventPublisher publisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void setUp() {
        reset(publisher);
        postRepository.deleteAll();
    }

    @Test
    void shouldPublishOnlyAfterPostTransactionCommits() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        PostResponse response = transaction.execute(status -> {
            PostResponse created = postCreationService.create(1L, new CreatePostRequest("Created post", 17L));
            assertThat(postRepository.findById(created.id())).isPresent();
            verifyNoInteractions(publisher);
            return created;
        });

        assertThat(response).isNotNull();
        assertThat(postRepository.findById(response.id())).isPresent();
        ArgumentCaptor<PostCreatedEvent> eventCaptor = ArgumentCaptor.forClass(PostCreatedEvent.class);
        verify(publisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(new PostCreatedEvent(response.id(), 1L, 17L));
    }

    @Test
    void shouldNotPublishOrKeepPostAfterRollback() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        PostResponse response = transaction.execute(status -> {
            PostResponse created = postCreationService.create(1L, new CreatePostRequest("Rolled back post", null));
            status.setRollbackOnly();
            return created;
        });

        assertThat(response).isNotNull();
        assertThat(postRepository.findById(response.id())).isEmpty();
        verifyNoInteractions(publisher);
    }

    @Test
    void shouldNotPublishIfSavingPostFails() {
        assertThatThrownBy(() -> postCreationService.create(1L, new CreatePostRequest(null, null)))
                .isInstanceOf(RuntimeException.class);

        assertThat(postRepository.count()).isZero();
        verifyNoInteractions(publisher);
    }

    @Test
    void shouldKeepCommittedPostWhenRedisPublicationFails() {
        doThrow(new RedisConnectionFailureException("Redis unavailable"))
                .when(publisher).publish(any(PostCreatedEvent.class));

        PostResponse response = postCreationService.create(1L, new CreatePostRequest("Committed post", null));

        assertThat(postRepository.findById(response.id())).isPresent();
        verify(publisher).publish(new PostCreatedEvent(response.id(), 1L, null));
    }

    @Test
    void shouldNotPublishEventOutsideTransaction() {
        applicationEventPublisher.publishEvent(new PostCreatedEvent(12345L, 1L, null));

        verifyNoInteractions(publisher);
    }

    @Configuration
    @EnableAutoConfiguration(exclude = {
            RedisAutoConfiguration.class,
            RedisRepositoriesAutoConfiguration.class,
            LiquibaseAutoConfiguration.class
    })
    @EntityScan(basePackageClasses = Post.class)
    @EnableJpaRepositories(basePackageClasses = PostRepository.class)
    @Import({PostCreationService.class, PostCreatedNotificationListener.class})
    static class TestConfig {
        @Bean
        PostCreatedEventPublisher postCreatedEventPublisher() {
            return mock(PostCreatedEventPublisher.class);
        }
    }
}
