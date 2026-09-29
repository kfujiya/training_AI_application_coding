package com.example.task_manager.service;

import com.example.task_manager.entity.Tag;
import com.example.task_manager.entity.User;
import com.example.task_manager.form.TagForm;
import com.example.task_manager.repository.TagRepository;
import com.example.task_manager.repository.TaskTagRepository;
import com.example.task_manager.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class TagService {
    private final TagRepository tagRepository;
    private final TaskTagRepository taskTagRepository;
    private final UserRepository userRepository;

    public TagService(TagRepository tagRepository, TaskTagRepository taskTagRepository, UserRepository userRepository) {
        this.tagRepository = tagRepository;
        this.taskTagRepository = taskTagRepository;
        this.userRepository = userRepository;
    }

    public List<Tag> findAllOwnedBy(String email) { return tagRepository.findAllByUserId(currentUser(email).getUserId()); }

    @Transactional
    public Tag create(TagForm form, String email) {
        User user = currentUser(email);
        String name = form.getName().trim();
        if (tagRepository.existsByUserIdAndName(user.getUserId(), name)) throw new DuplicateTagException();
        Tag tag = new Tag();
        tag.setUserId(user.getUserId());
        tag.setName(name);
        return tagRepository.save(tag);
    }

    public Tag findOwnedById(Long tagId, String email) {
        return tagRepository.findByTagIdAndUserId(tagId, currentUser(email).getUserId())
                .orElseThrow(TagNotFoundException::new);
    }

    @Transactional
    public void update(Long tagId, TagForm form, String email) {
        Tag tag = findOwnedById(tagId, email);
        String name = form.getName().trim();
        if (tagRepository.existsByUserIdAndNameAndTagIdNot(tag.getUserId(), name, tagId)) throw new DuplicateTagException();
        tag.setName(name);
        tagRepository.save(tag);
    }

    @Transactional
    public void delete(Long tagId, String email) {
        Tag tag = findOwnedById(tagId, email);
        taskTagRepository.deleteAllByIdTagId(tag.getTagId());
        tagRepository.delete(tag);
    }

    private User currentUser(String email) { return userRepository.findByEmail(email).orElseThrow(TagNotFoundException::new); }
    public static class TagNotFoundException extends RuntimeException { private static final long serialVersionUID = 1L; }
    public static class DuplicateTagException extends RuntimeException { private static final long serialVersionUID = 1L; }
}
