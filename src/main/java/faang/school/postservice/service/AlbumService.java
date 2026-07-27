package faang.school.postservice.service;

import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.config.context.UserContext;
import faang.school.postservice.dto.album.AlbumDto;
import faang.school.postservice.exception.DataValidationException;
import faang.school.postservice.mapper.AlbumMapper;
import faang.school.postservice.model.Album;
import faang.school.postservice.model.enums.Visibility;
import faang.school.postservice.repository.AlbumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final UserServiceClient userServiceClient;
    private final UserContext userContext;
    private final AlbumMapper albumMapper;

    @Transactional(readOnly = true)
    public AlbumDto getAlbum(long albumId) {
        Album album = albumRepository.findByIdWithPosts(albumId)
                .orElseThrow(() -> new DataValidationException("Album with id " + albumId + " not found"));

        long currentUserId = userContext.getUserId();

        checkVisibility(album, currentUserId);

        return albumMapper.toAlbumDto(album);
    }

    private void checkVisibility(Album album, long currentUserId) {
        if (album.getAuthorId() == currentUserId) {
            return;
        }

        Visibility visibility = album.getVisibility();

        if (visibility == Visibility.ALL_USERS) {
            return;
        }

        if (visibility == Visibility.SELECTED_USERS) {
            if (album.getAllowedUserIds() == null || !album.getAllowedUserIds().contains(currentUserId)) {
                throw new DataValidationException("You do not have access to this album");
            }

            return;
        }

        if (visibility == Visibility.FOLLOWERS || visibility == Visibility.FRIENDS) {
            List<Long> followingIds = userServiceClient.getFollowingUserIds(currentUserId);

            boolean isFollower = followingIds.contains(album.getAuthorId());

            if (!isFollower) {
                throw new DataValidationException("This album is only visible to followers of the author");
            }
            //friends logic for future
        }
    }
}