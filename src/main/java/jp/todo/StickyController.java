package jp.todo;

import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin
@Transactional
public class StickyController {
    private final StickyService service;
    public StickyController(StickyService service) { this.service = service; }

    public record TaskView(Long id, String title, boolean completed, LocalDateTimeView createdAt) {}
    public record LocalDateTimeView(String value) {}
    public record NoteView(Long id, String type, LocalDate noteDate, boolean settled, Integer finalProgress, String unfinishedSummary, int completedCount, int totalCount, List<TaskView> tasks) {}
    public record CreateTask(String title) {}
    public record UpdateTask(String title) {}

    @GetMapping("/stickies") public List<NoteView> list() { return service.listNotes().stream().map(StickyController::view).toList(); }
    @PostMapping("/stickies/long-term") public NoteView createLongTerm() { return view(service.createLongTerm()); }
    @PostMapping("/stickies/{noteId}/tasks") public NoteView addTask(@PathVariable Long noteId, @RequestBody CreateTask body) { service.addTask(noteId, body.title()); return view(service.listNotes().stream().filter(n -> n.getId().equals(noteId)).findFirst().orElseThrow()); }
    @PatchMapping("/stickies/{noteId}/tasks/{taskId}") public NoteView toggle(@PathVariable Long noteId, @PathVariable Long taskId) { service.toggleTask(noteId, taskId); return view(service.listNotes().stream().filter(n -> n.getId().equals(noteId)).findFirst().orElseThrow()); }
    @PutMapping("/stickies/{noteId}/tasks/{taskId}") public NoteView updateTask(@PathVariable Long noteId, @PathVariable Long taskId, @RequestBody UpdateTask body) { service.updateTask(noteId, taskId, body.title()); return view(service.listNotes().stream().filter(n -> n.getId().equals(noteId)).findFirst().orElseThrow()); }
    @PostMapping("/stickies/{noteId}/settle") public NoteView settle(@PathVariable Long noteId) { return view(service.settle(noteId)); }

    private static NoteView view(StickyNote note) {
        List<TaskView> taskViews = note.getTasks().stream().sorted((a,b) -> Boolean.compare(a.isCompleted(), b.isCompleted())).map(t -> new TaskView(t.getId(), t.getTitle(), t.isCompleted(), new LocalDateTimeView(t.getCreatedAt().toString()))).toList();
        int completed = (int) taskViews.stream().filter(TaskView::completed).count();
        return new NoteView(note.getId(), note.getType().name(), note.getNoteDate(), note.isSettled(), note.getFinalProgress(), note.getUnfinishedSummary(), completed, taskViews.size(), taskViews);
    }
}
