package com.example.household.service;

import com.example.household.model.Expense;
import com.example.household.repository.ExpenseRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public ExpenseService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    public List<Expense> findAll() {
        return expenseRepository.findAll();
    }

    public BigDecimal findTotalAmount() {
        return expenseRepository.findTotalAmount();
    }

    public Expense findById(long id) {
        return expenseRepository.findById(id);
    }

    public void register(LocalDate expenseDate, String category,
                         String description, BigDecimal amount) {
        expenseRepository.insert(expenseDate, category, description, amount);
    }

    public int update(long id, LocalDate expenseDate, String category,
                      String description, BigDecimal amount) {
        return expenseRepository.update(id, expenseDate, category, description, amount);
    }

    public int delete(long id) {
        return expenseRepository.delete(id);
    }
}
