package faang.school.postservice.dto.post;

public record PostResponse(
        Long id,
        String content,
        Long authorId,
        Long projectId,
        boolean published
) {
}
