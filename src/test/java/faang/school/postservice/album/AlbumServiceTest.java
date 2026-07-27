package faang.school.postservice.album;

import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.config.context.UserContext;
import faang.school.postservice.dto.album.AlbumDto;
import faang.school.postservice.exception.DataValidationException;
import faang.school.postservice.mapper.AlbumMapper;
import faang.school.postservice.model.Album;
import faang.school.postservice.model.enums.Visibility;
import faang.school.postservice.repository.AlbumRepository;
import faang.school.postservice.service.AlbumService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlbumServiceTest {

    @Mock
    private AlbumRepository albumRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private UserContext userContext;

    @Mock
    private AlbumMapper albumMapper;

    @InjectMocks
    private AlbumService albumService;

    private Album album;
    private AlbumDto albumDto;

    private final long albumId = 1L;
    private final long authorId = 10L;
    private final long strangerId = 42L;

    @BeforeEach
    void setUp() {
        album = Album.builder()
                .id(albumId)
                .title("Test Album")
                .description("Test Description")
                .authorId(authorId)
                .visibility(Visibility.ALL_USERS)
                .allowedUserIds(new ArrayList<>())
                .build();

        albumDto = AlbumDto.builder()
                .id(albumId)
                .title("Test Album")
                .description("Test Description")
                .authorId(authorId)
                .visibility(Visibility.ALL_USERS)
                .allowedUserIds(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Should throw exception when album is not found by ID")
    void testGetAlbum_WhenAlbumNotFound_ShouldThrowException() {
        when(albumRepository.findByIdWithPosts(albumId)).thenReturn(Optional.empty());

        DataValidationException exception = assertThrows(
                DataValidationException.class,
                () -> albumService.getAlbum(albumId));

        assertEquals("Album with id " + albumId + " not found", exception.getMessage());
        verify(albumRepository).findByIdWithPosts(albumId);
        verifyNoInteractions(userContext, userServiceClient, albumMapper);
    }

    @Test
    @DisplayName("Should successfully return album when current user is the author")
    void testGetAlbum_WhenCurrentUserIsAuthor_ShouldReturnAlbum() {
        album.setVisibility(Visibility.SELECTED_USERS);

        when(albumRepository.findByIdWithPosts(albumId)).thenReturn(Optional.of(album));
        when(userContext.getUserId()).thenReturn(authorId);
        when(albumMapper.toAlbumDto(album)).thenReturn(albumDto);

        AlbumDto result = albumService.getAlbum(albumId);

        assertNotNull(result);
        assertEquals(authorId, result.getAuthorId());
        assertEquals(album.getTitle(), result.getTitle());
        verify(albumRepository).findByIdWithPosts(albumId);
        verify(userContext).getUserId();
        verify(albumMapper).toAlbumDto(album);
        verifyNoInteractions(userServiceClient);
    }

    @Test
    @DisplayName("Should successfully return album when visibility is ALL_USERS")
    void testGetAlbum_WhenVisibilityIsAllUsers_ShouldReturnAlbum() {
        album.setVisibility(Visibility.ALL_USERS);

        when(albumRepository.findByIdWithPosts(albumId)).thenReturn(Optional.of(album));
        when(userContext.getUserId()).thenReturn(strangerId);
        when(albumMapper.toAlbumDto(album)).thenReturn(albumDto);

        AlbumDto result = albumService.getAlbum(albumId);

        assertNotNull(result);
        assertEquals(album.getTitle(), result.getTitle());
        verify(albumRepository).findByIdWithPosts(albumId);
        verify(userContext).getUserId();
        verifyNoInteractions(userServiceClient);
    }

    @Test
    @DisplayName("Should successfully return album when user is in allowed users list")
    void testGetAlbum_WhenUserInSelectedUsersList_ShouldReturnAlbum() {
        album.setVisibility(Visibility.SELECTED_USERS);
        album.setAllowedUserIds(List.of(strangerId, 99L));

        when(albumRepository.findByIdWithPosts(albumId)).thenReturn(Optional.of(album));
        when(userContext.getUserId()).thenReturn(strangerId);
        when(albumMapper.toAlbumDto(album)).thenReturn(albumDto);

        AlbumDto result = albumService.getAlbum(albumId);

        assertNotNull(result);
        assertEquals(album.getTitle(), result.getTitle());
        verify(albumRepository).findByIdWithPosts(albumId);
        verify(userContext).getUserId();
        verifyNoInteractions(userServiceClient);
    }

    @Test
    @DisplayName("Should throw exception when visibility is SELECTED_USERS and user is not in the list")
    void testGetAlbum_WhenUserNotInSelectedUsersList_ShouldThrowException() {
        album.setVisibility(Visibility.SELECTED_USERS);
        album.setAllowedUserIds(List.of(99L));

        when(albumRepository.findByIdWithPosts(albumId)).thenReturn(Optional.of(album));
        when(userContext.getUserId()).thenReturn(strangerId);

        DataValidationException exception = assertThrows(
                DataValidationException.class,
                () -> albumService.getAlbum(albumId));

        assertEquals("You do not have access to this album", exception.getMessage());
        verifyNoInteractions(userServiceClient, albumMapper);
    }

    @Test
    @DisplayName("Should successfully return album when user is a follower of the author")
    void testGetAlbum_WhenUserIsFollower_ShouldReturnAlbum() {
        album.setVisibility(Visibility.FOLLOWERS);

        when(albumRepository.findByIdWithPosts(albumId)).thenReturn(Optional.of(album));
        when(userContext.getUserId()).thenReturn(strangerId);
        when(userServiceClient.getFollowingUserIds(strangerId)).thenReturn(List.of(authorId, 55L));
        when(albumMapper.toAlbumDto(album)).thenReturn(albumDto);

        AlbumDto result = albumService.getAlbum(albumId);

        assertNotNull(result);
        assertEquals(album.getTitle(), result.getTitle());
        verify(userServiceClient).getFollowingUserIds(strangerId);
        verify(albumMapper).toAlbumDto(album);
    }

    @Test
    @DisplayName("Should throw exception when user is not a follower of the author")
    void testGetAlbum_WhenUserIsNotFollower_ShouldThrowException() {
        album.setVisibility(Visibility.FOLLOWERS);

        when(albumRepository.findByIdWithPosts(albumId)).thenReturn(Optional.of(album));
        when(userContext.getUserId()).thenReturn(strangerId);
        when(userServiceClient.getFollowingUserIds(strangerId)).thenReturn(List.of(55L, 88L));

        DataValidationException exception = assertThrows(
                DataValidationException.class,
                () -> albumService.getAlbum(albumId));

        assertEquals("This album is only visible to followers of the author", exception.getMessage());
        verify(userServiceClient).getFollowingUserIds(strangerId);
        verifyNoInteractions(albumMapper);
    }

    @Test
    @DisplayName("Should throw exception when visibility is SELECTED_USERS and allowed users list is null")
    void testGetAlbum_WhenAllowedUsersListIsNull_ShouldThrowException() {
        album.setVisibility(Visibility.SELECTED_USERS);
        album.setAllowedUserIds(null);

        when(albumRepository.findByIdWithPosts(albumId)).thenReturn(Optional.of(album));
        when(userContext.getUserId()).thenReturn(strangerId);

        assertThrows(
                DataValidationException.class,
                () -> albumService.getAlbum(albumId)
        );
        verifyNoInteractions(userServiceClient, albumMapper);
    }

    @Test
    @DisplayName("Should successfully return album when visibility is FRIENDS and user is a follower")
    void testGetAlbum_WhenVisibilityIsFriendsAndUserIsFollower_ShouldReturnAlbum() {
        album.setVisibility(Visibility.FRIENDS);

        when(albumRepository.findByIdWithPosts(albumId)).thenReturn(Optional.of(album));
        when(userContext.getUserId()).thenReturn(strangerId);
        when(userServiceClient.getFollowingUserIds(strangerId)).thenReturn(List.of(authorId));
        when(albumMapper.toAlbumDto(album)).thenReturn(albumDto);

        AlbumDto result = albumService.getAlbum(albumId);

        assertNotNull(result);
        verify(userServiceClient, times(1)).getFollowingUserIds(strangerId);
    }

    @Test
    @DisplayName("Should skip followers block when visibility is null")
    void testCheckVisibility_WhenVisibilityIsNull_ShouldSkipBlock() {
        album.setVisibility(null);

        when(albumRepository.findByIdWithPosts(albumId)).thenReturn(Optional.of(album));
        when(userContext.getUserId()).thenReturn(strangerId);
        when(albumMapper.toAlbumDto(album)).thenReturn(albumDto);

        AlbumDto result = albumService.getAlbum(albumId);

        assertNotNull(result);
        verifyNoInteractions(userServiceClient);
    }
}