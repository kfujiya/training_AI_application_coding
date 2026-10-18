package jp.todo;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class StickyService {
    private final StickyNoteRepository notes;
    private final TodoTaskRepository tasks;
    public StickyService(StickyNoteRepository notes, TodoTaskRepository tasks) { this.notes = notes; this.tasks = tasks; }

    @Transactional
    public StickyNote ensureToday() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Tokyo"));
        return notes.findByTypeAndNoteDate(StickyNote.Type.DAILY, today)
                .orElseGet(() -> notes.save(new StickyNote(StickyNote.Type.DAILY, today)));
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Tokyo")
    @Transactional
    public void createDailyNoteAtMidnight() { ensureToday(); }

    @Transactional
    public List<StickyNote> listNotes() {
        ensureToday();
        return notes.findAllByOrderByNoteDateDesc();
    }

    @Transactional
    public StickyNote createLongTerm() { return notes.save(new StickyNote(StickyNote.Type.LONG_TERM, LocalDate.now(ZoneId.of("Asia/Tokyo")))); }

    @Transactional
    public TodoTask addTask(Long noteId, String title) {
        if (title == null || title.trim().isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "タスク名を入力してください");
        StickyNote note = getNote(noteId);
        ensureEditable(note);
        return tasks.save(new TodoTask(note, title.trim()));
    }

    @Transactional
    public TodoTask toggleTask(Long noteId, Long taskId) {
        StickyNote note = getNote(noteId);
        ensureEditable(note);
        TodoTask task = tasks.findByIdAndNoteId(taskId, noteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "タスクが見つかりません"));
        task.toggle();
        return task;
    }

    @Transactional
    public TodoTask updateTask(Long noteId, Long taskId, String title) {
        if (title == null || title.trim().isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "タスク名を入力してください");
        StickyNote note = getNote(noteId);
        ensureEditable(note);
        TodoTask task = tasks.findByIdAndNoteId(taskId, noteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "タスクが見つかりません"));
        task.rename(title.trim());
        return task;
    }

    @Transactional
    public StickyNote settle(Long noteId) {
        StickyNote note = getNote(noteId);
        if (note.getType() != StickyNote.Type.DAILY) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "日次付箋のみ締められます");
        ensureEditable(note);
        int total = note.getTasks().size();
        long complete = note.getTasks().stream().filter(TodoTask::isCompleted).count();
        int percent = total == 0 ? 0 : (int) Math.round(complete * 100.0 / total);
        String summary = note.getTasks().stream().filter(task -> !task.isCompleted()).map(TodoTask::getTitle).reduce((a, b) -> a + "\n" + b).orElse("未完了タスクはありません");
        note.settle(percent, summary);
        return note;
    }

    private StickyNote getNote(Long id) { return notes.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "付箋が見つかりません")); }
    private void ensureEditable(StickyNote note) {
        if (note.getType() == StickyNote.Type.DAILY && note.isSettled()) throw new ResponseStatusException(HttpStatus.CONFLICT, "締め済みの日次付箋は編集できません");
    }
}
