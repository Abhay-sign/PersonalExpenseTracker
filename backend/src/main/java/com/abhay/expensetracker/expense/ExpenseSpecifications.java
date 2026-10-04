package com.abhay.expensetracker.expense;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/** Small reusable WHERE-clause pieces. A null argument means "no filter" (returns null = ignored). */
final class ExpenseSpecifications {

    private ExpenseSpecifications() {
    }

    static Specification<Expense> ownedBy(Long userId) {
        return (root, q, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    static Specification<Expense> dateFrom(LocalDate from) {
        return from == null ? null : (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("expenseDate"), from);
    }

    static Specification<Expense> dateTo(LocalDate to) {
        return to == null ? null : (root, q, cb) -> cb.lessThanOrEqualTo(root.get("expenseDate"), to);
    }

    static Specification<Expense> inCategory(Long categoryId) {
        return categoryId == null ? null : (root, q, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }
}
