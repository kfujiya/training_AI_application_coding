package com.example.task_manager.controller;

import com.example.task_manager.entity.Tag;
import com.example.task_manager.form.TagForm;
import com.example.task_manager.service.TagService;
import com.example.task_manager.service.TagService.DuplicateTagException;
import com.example.task_manager.service.TagService.TagNotFoundException;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/tags")
public class TagController {
    private final TagService tagService;
    public TagController(TagService tagService) { this.tagService = tagService; }

    @GetMapping
    public String list(Authentication authentication, Model model) {
        model.addAttribute("tags", tagService.findAllOwnedBy(authentication.getName()));
        model.addAttribute("tagForm", new TagForm());
        return "tags/list";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("tagForm") TagForm form, BindingResult result,
                         Authentication authentication, Model model) {
        if (result.hasErrors()) return listWithForm(authentication, model);
        try {
            tagService.create(form, authentication.getName());
        } catch (DuplicateTagException e) {
            result.rejectValue("name", "duplicate", "同じ名前のタグは登録できません。");
            return listWithForm(authentication, model);
        }
        return "redirect:/tags";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Authentication authentication, Model model) {
        try {
            Tag tag = tagService.findOwnedById(id, authentication.getName());
            TagForm form = new TagForm();
            form.setName(tag.getName());
            model.addAttribute("tagForm", form);
            model.addAttribute("tagId", id);
            return "tags/form";
        } catch (TagNotFoundException e) { return "redirect:/tags"; }
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("tagForm") TagForm form,
                         BindingResult result, Authentication authentication, Model model) {
        if (result.hasErrors()) { model.addAttribute("tagId", id); return "tags/form"; }
        try { tagService.update(id, form, authentication.getName()); }
        catch (TagNotFoundException e) { return "redirect:/tags"; }
        catch (DuplicateTagException e) { result.rejectValue("name", "duplicate", "同じ名前のタグは登録できません。"); model.addAttribute("tagId", id); return "tags/form"; }
        return "redirect:/tags";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication) {
        try { tagService.delete(id, authentication.getName()); }
        catch (TagNotFoundException ignored) { }
        return "redirect:/tags";
    }

    private String listWithForm(Authentication authentication, Model model) {
        model.addAttribute("tags", tagService.findAllOwnedBy(authentication.getName()));
        return "tags/list";
    }
}
