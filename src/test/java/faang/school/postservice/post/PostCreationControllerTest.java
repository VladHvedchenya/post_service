package faang.school.postservice.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.config.context.UserContext;
import faang.school.postservice.config.context.UserHeaderFilter;
import faang.school.postservice.controller.post.PostCreationController;
import faang.school.postservice.dto.post.CreatePostRequest;
import faang.school.postservice.dto.post.PostResponse;
import faang.school.postservice.service.post.PostCreationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PostCreationControllerTest {
    @Mock
    private PostCreationService postCreationService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PostCreationController(postCreationService))
                .addFilters(new UserHeaderFilter(new UserContext()))
                .build();
    }

    @Test
    void shouldCreatePostUsingAuthorFromHeader() throws Exception {
        CreatePostRequest request = new CreatePostRequest("New post", null);
        when(postCreationService.create(1L, request))
                .thenReturn(new PostResponse(105L, "New post", 1L, null, false));

        mockMvc.perform(post("/api/v1/posts")
                        .header("x-user-id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(105))
                .andExpect(jsonPath("$.authorId").value(1))
                .andExpect(jsonPath("$.content").value("New post"))
                .andExpect(jsonPath("$.published").value(false));

        verify(postCreationService).create(1L, request);
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @ValueSource(strings = {" ", "\t"})
    void shouldRejectBlankContent(String content) throws Exception {
        assertInvalidRequest(new CreatePostRequest(content, null));
    }

    @Test
    void shouldRejectContentLongerThanDatabaseLimit() throws Exception {
        assertInvalidRequest(new CreatePostRequest("a".repeat(4097), null));
    }

    @Test
    void shouldAcceptMaximumContentLengthAndProject() throws Exception {
        CreatePostRequest request = new CreatePostRequest("a".repeat(4096), 17L);
        when(postCreationService.create(1L, request))
                .thenReturn(new PostResponse(105L, request.content(), 1L, 17L, false));

        mockMvc.perform(post("/api/v1/posts")
                        .header("x-user-id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(17));
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void shouldRejectNonPositiveProject(Long projectId) throws Exception {
        assertInvalidRequest(new CreatePostRequest("New post", projectId));
    }

    @Test
    void shouldRequireAuthorHeader() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"New post\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(postCreationService);
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"abc", "0", "-1", "9223372036854775808"})
    void shouldRejectInvalidAuthorHeader(String authorId) throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .header("x-user-id", authorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"New post\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(postCreationService);
    }

    private void assertInvalidRequest(CreatePostRequest request) throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .header("x-user-id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(postCreationService);
    }
}
