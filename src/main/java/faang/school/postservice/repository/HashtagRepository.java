package faang.school.postservice.repository;

import faang.school.postservice.model.Hashtag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HashtagRepository extends JpaRepository<Hashtag, Long> {

    @Query("SELECT h" +
            " FROM Hashtag h" +
            " WHERE LOWER(h.name) = LOWER(:name)")
    Optional<Hashtag> findByNameIgnoreCase(@Param("name") String name);

    @Query(value = """
            SELECT p.* FROM post p
            JOIN post_hashtag ph ON p.id = ph.post_id
            WHERE ph.hashtag_id = :hashtagId
            ORDER BY ph.post_id DESC
            """, nativeQuery = true)
    List<Object[]> findPostsByHashtagIdChronological(@Param("hashtagId") Long hashtagId);
}