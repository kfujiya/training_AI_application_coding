package com.example.household.repository;

import com.example.household.model.Expense;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Repository
public class ExpenseRepository {

    private final JdbcTemplate jdbcTemplate;

    public ExpenseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Expense> findAll() {
        String sql = "SELECT id, expense_date, category, description, amount "
                + "FROM public.expenses "
                + "ORDER BY expense_date DESC, id DESC";

        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> mapExpense(resultSet));
    }

    public Expense findById(long id) {
        String sql = "SELECT id, expense_date, category, description, amount "
                + "FROM public.expenses WHERE id = ?";
        List<Expense> expenses = jdbcTemplate.query(
                sql,
                (resultSet, rowNumber) -> mapExpense(resultSet),
                id
        );
        return expenses.isEmpty() ? null : expenses.get(0);
    }

    public BigDecimal findTotalAmount() {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM public.expenses";
        return jdbcTemplate.queryForObject(sql, BigDecimal.class);
    }

    public int insert(LocalDate expenseDate, String category,
                      String description, BigDecimal amount) {
        String sql = "INSERT INTO public.expenses "
                + "(expense_date, category, description, amount) "
                + "VALUES (?, ?, ?, ?)";
        return jdbcTemplate.update(sql, Date.valueOf(expenseDate), category, description, amount);
    }

    public int update(long id, LocalDate expenseDate, String category,
                      String description, BigDecimal amount) {
        String sql = "UPDATE public.expenses "
                + "SET expense_date = ?, category = ?, description = ?, amount = ? "
                + "WHERE id = ?";
        return jdbcTemplate.update(
                sql,
                Date.valueOf(expenseDate),
                category,
                description,
                amount,
                id
        );
    }

    public int delete(long id) {
        String sql = "DELETE FROM public.expenses WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }

    private Expense mapExpense(ResultSet resultSet) throws SQLException {
        return new Expense(
                resultSet.getLong("id"),
                resultSet.getDate("expense_date").toLocalDate(),
                resultSet.getString("category"),
                resultSet.getString("description"),
                resultSet.getBigDecimal("amount")
        );
    }
}
