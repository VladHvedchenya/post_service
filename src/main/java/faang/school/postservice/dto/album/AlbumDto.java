package faang.school.postservice.dto.album;

import faang.school.postservice.model.enums.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AlbumDto {
    private Long id;

    @NotBlank(message = "Title cannot be empty")
    @Size(max = 256, message = "Title cannot exceed 256 characters")
    private String title;

    @NotBlank(message = "Description cannot be empty")
    @Size(max = 4096, message = "Description cannot exceed 4096 characters")
    private String description;

    private Long authorId;

    @NotNull(message = "Visibility status must be specified")
    private Visibility visibility;

    private List<Long> allowedUserIds;
}