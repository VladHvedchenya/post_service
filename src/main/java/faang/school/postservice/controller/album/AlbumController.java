package faang.school.postservice.controller.album;

import faang.school.postservice.dto.album.AlbumDto;
import faang.school.postservice.service.AlbumService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/albums")
public class AlbumController {
    private final AlbumService albumService;

    @GetMapping("/{albumId}")
    public AlbumDto getAlbum(@PathVariable long albumId) {
        log.info("Received request to fetch album with id: {}", albumId);
        return albumService.getAlbum(albumId);
    }
}