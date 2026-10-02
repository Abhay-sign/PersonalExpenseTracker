package com.abhay.expensetracker.expense;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * REPOSITORY: you write only the interface, Spring Data generates the implementation at startup.
 * JpaRepository already gives you save(), findById(), findAll(), deleteById(), count() ...
 */
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // "Derived query": Spring reads the method NAME and builds the SQL:
    // SELECT * FROM expenses WHERE expense_date BETWEEN ? AND ? ORDER BY expense_date DESC
    List<Expense> findByExpenseDateBetweenOrderByExpenseDateDesc(LocalDate from, LocalDate to);
}
