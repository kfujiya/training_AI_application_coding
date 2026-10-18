package jp.todo;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sticky_notes")
public class StickyNote {
    public enum Type { DAILY, LONG_TERM }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Type type;
    @Column(name = "note_date", nullable = false) private LocalDate noteDate;
    @Column(name = "daily_date", unique = true) private LocalDate dailyDate;
    @Column(nullable = false) private boolean settled = false;
    private Integer finalProgress;
    @Column(columnDefinition = "text") private String unfinishedSummary;
    @OneToMany(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC") private List<TodoTask> tasks = new ArrayList<>();

    protected StickyNote() {}
    public StickyNote(Type type, LocalDate noteDate) { this.type = type; this.noteDate = noteDate; this.dailyDate = type == Type.DAILY ? noteDate : null; }
    public Long getId() { return id; }
    public Type getType() { return type; }
    public LocalDate getNoteDate() { return noteDate; }
    public boolean isSettled() { return settled; }
    public Integer getFinalProgress() { return finalProgress; }
    public String getUnfinishedSummary() { return unfinishedSummary; }
    public List<TodoTask> getTasks() { return tasks; }
    public void settle(int progress, String summary) { settled = true; finalProgress = progress; unfinishedSummary = summary; }
}
