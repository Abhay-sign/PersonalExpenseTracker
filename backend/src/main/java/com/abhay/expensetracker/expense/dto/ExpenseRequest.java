package com.abhay.expensetracker.expense.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for incoming JSON (POST / PUT). Validated by @Valid in the controller.
 * We never accept the Entity directly, so clients can't set id/createdAt themselves.
 */
public record ExpenseRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 100, message = "Title must be at most 100 characters")
        String title,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        @Digits(integer = 10, fraction = 2, message = "Amount can have at most 2 decimal places")
        BigDecimal amount,

        @NotNull(message = "Date is required")
        @PastOrPresent(message = "Date cannot be in the future")
        LocalDate expenseDate,

        @Size(max = 500, message = "Note must be at most 500 characters")
        String note,

        // optional: omit or null for "uncategorized"
        Long categoryId
) {
}
