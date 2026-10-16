package faang.school.postservice.dto.event;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

public record PostCreatedEvent(
        @NotNull
        Long postId,
        @NotNull
        Long authorId,
        @Nullable
        Long projectId
)
{}