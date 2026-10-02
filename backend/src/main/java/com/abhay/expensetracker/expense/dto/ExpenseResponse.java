package com.abhay.expensetracker.expense.dto;

import com.abhay.expensetracker.expense.Expense;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** DTO for outgoing JSON. Controls exactly what the frontend sees. */
public record ExpenseResponse(
        Long id,
        String title,
        BigDecimal amount,
        LocalDate expenseDate,
        String note,
        Instant createdAt,
        Instant updatedAt
) {
    public static ExpenseResponse from(Expense e) {
        return new ExpenseResponse(
                e.getId(), e.getTitle(), e.getAmount(), e.getExpenseDate(),
                e.getNote(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
