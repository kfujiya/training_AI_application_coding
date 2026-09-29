package com.example.task_manager.repository;
import com.example.task_manager.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.Collection;
public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findAllByUserId(Long userId);
    Optional<Tag> findByTagIdAndUserId(Long tagId, Long userId);
    boolean existsByUserIdAndName(Long userId, String name);
    boolean existsByUserIdAndNameAndTagIdNot(Long userId, String name, Long tagId);
    List<Tag> findAllByTagIdInAndUserId(Collection<Long> tagIds, Long userId);
}
