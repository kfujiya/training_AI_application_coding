package jp.todo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StickyNoteRepository extends JpaRepository<StickyNote, Long> {
    Optional<StickyNote> findByTypeAndNoteDate(StickyNote.Type type, LocalDate noteDate);
    List<StickyNote> findAllByOrderByNoteDateDesc();
}
