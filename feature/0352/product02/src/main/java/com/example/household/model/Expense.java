package com.example.household.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Expense {

    private final long id;
    private final LocalDate expenseDate;
    private final String category;
    private final String description;
    private final BigDecimal amount;

    public Expense(long id, LocalDate expenseDate, String category,
                   String description, BigDecimal amount) {
        this.id = id;
        this.expenseDate = expenseDate;
        this.category = category;
        this.description = description;
        this.amount = amount;
    }

    public long getId() {
        return id;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
