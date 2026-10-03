package com.abhay.expensetracker.expense.dto;

import java.math.BigDecimal;

/** One row of the monthly report. Built directly by the JPQL query in ExpenseRepository. */
public record MonthlyTotal(int year, int month, BigDecimal total, long count) {
}
