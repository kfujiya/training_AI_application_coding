package jp.todo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "todo_tasks")
public class TodoTask {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "note_id", nullable = false) private StickyNote note;
    @Column(nullable = false, length = 300) private String title;
    @Column(nullable = false) private boolean completed = false;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    protected TodoTask() {}
    public TodoTask(StickyNote note, String title) { this.note = note; this.title = title; this.createdAt = LocalDateTime.now(); }
    @PrePersist void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public boolean isCompleted() { return completed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public StickyNote getNote() { return note; }
    public void toggle() { completed = !completed; }
    public void rename(String title) { this.title = title; }
}
