package com.example.task_manager.repository;
import com.example.task_manager.entity.TaskTag;
import com.example.task_manager.entity.TaskTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Collection;
public interface TaskTagRepository extends JpaRepository<TaskTag, TaskTagId> {
    List<TaskTag> findAllByIdTaskId(Long taskId);
    void deleteAllByIdTaskId(Long taskId);
    void deleteAllByIdTagId(Long tagId);
    List<TaskTag> findAllByIdTaskIdIn(Collection<Long> taskIds);
}
