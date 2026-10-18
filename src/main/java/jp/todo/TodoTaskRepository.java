package jp.todo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TodoTaskRepository extends JpaRepository<TodoTask, Long> {
    Optional<TodoTask> findByIdAndNoteId(Long id, Long noteId);
}
