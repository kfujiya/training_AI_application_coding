package com.example.household.controller;

import com.example.household.service.ExpenseService;
import com.example.household.form.ExpenseForm;
import com.example.household.model.Expense;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Controller
public class ExpenseController {

    private static final List<String> CATEGORIES = Arrays.asList(
            "食費", "交通費", "日用品", "娯楽", "その他"
    );

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping({"/", "/expenses"})
    public String listExpenses(Model model) {
        model.addAttribute("expenses", expenseService.findAll());
        model.addAttribute("totalAmount", expenseService.findTotalAmount());
        return "expenses/list";
    }

    @GetMapping("/expenses/new")
    public String showCreateForm(Model model) {
        prepareFormModel(model, new ExpenseForm(), null, false);
        return "expenses/form";
    }

    @PostMapping("/expenses/new")
    public String createExpense(@ModelAttribute("expenseForm") ExpenseForm form,
                                BindingResult bindingResult,
                                Model model) {
        validate(form, bindingResult);
        if (bindingResult.hasErrors()) {
            prepareFormModel(model, form, null, false);
            return "expenses/form";
        }

        expenseService.register(
                form.getExpenseDate(),
                form.getCategory(),
                form.getDescription().trim(),
                form.getAmount()
        );
        return "redirect:/expenses";
    }

    @GetMapping("/expenses/{id}/edit")
    public String showEditForm(@PathVariable long id, Model model) {
        Expense expense = expenseService.findById(id);
        if (expense == null) {
            return "redirect:/expenses";
        }

        ExpenseForm form = new ExpenseForm();
        form.setExpenseDate(expense.getExpenseDate());
        form.setCategory(expense.getCategory());
        form.setDescription(expense.getDescription());
        form.setAmount(expense.getAmount());
        prepareFormModel(model, form, id, true);
        return "expenses/form";
    }

    @PostMapping("/expenses/{id}/edit")
    public String updateExpense(@PathVariable long id,
                                @ModelAttribute("expenseForm") ExpenseForm form,
                                BindingResult bindingResult,
                                Model model) {
        validate(form, bindingResult);
        if (bindingResult.hasErrors()) {
            prepareFormModel(model, form, id, true);
            return "expenses/form";
        }

        expenseService.update(
                id,
                form.getExpenseDate(),
                form.getCategory(),
                form.getDescription().trim(),
                form.getAmount()
        );
        return "redirect:/expenses";
    }

    @PostMapping("/expenses/{id}/delete")
    public String deleteExpense(@PathVariable long id) {
        expenseService.delete(id);
        return "redirect:/expenses";
    }

    private void prepareFormModel(Model model, ExpenseForm form, Long expenseId, boolean editMode) {
        model.addAttribute("expenseForm", form);
        model.addAttribute("categories", CATEGORIES);
        model.addAttribute("pageTitle", editMode ? "支出編集" : "支出登録");
        model.addAttribute("submitLabel", editMode ? "更新" : "登録");
        model.addAttribute("formAction", editMode
                ? "/expenses/" + expenseId + "/edit"
                : "/expenses/new");
    }

    private void validate(ExpenseForm form, BindingResult bindingResult) {
        if (form.getExpenseDate() == null && !bindingResult.hasFieldErrors("expenseDate")) {
            bindingResult.rejectValue("expenseDate", "required", "支出日を入力してください。");
        }

        if (form.getCategory() == null || !CATEGORIES.contains(form.getCategory())) {
            bindingResult.rejectValue("category", "invalid", "カテゴリを選択してください。");
        }

        if (form.getDescription() == null || form.getDescription().trim().isEmpty()) {
            bindingResult.rejectValue("description", "required", "内容を入力してください。");
        } else if (form.getDescription().trim().length() > 200) {
            bindingResult.rejectValue("description", "length", "内容は200文字以内で入力してください。");
        }

        BigDecimal amount = form.getAmount();
        if (amount == null) {
            if (!bindingResult.hasFieldErrors("amount")) {
                bindingResult.rejectValue("amount", "required", "金額を入力してください。");
            }
        } else if (amount.compareTo(BigDecimal.ZERO) <= 0
                || amount.compareTo(new BigDecimal("9999999999.99")) > 0
                || amount.scale() > 2) {
            bindingResult.rejectValue("amount", "invalid", "金額は0より大きく、整数部10桁・小数部2桁以内で入力してください。");
        }
    }
}
