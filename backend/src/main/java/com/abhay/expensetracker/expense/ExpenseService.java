package com.abhay.expensetracker.expense;

import com.abhay.expensetracker.category.Category;
import com.abhay.expensetracker.category.CategoryService;
import com.abhay.expensetracker.common.PageResponse;
import com.abhay.expensetracker.common.ResourceNotFoundException;
import com.abhay.expensetracker.expense.dto.ExpenseRequest;
import com.abhay.expensetracker.expense.dto.ExpenseResponse;
import com.abhay.expensetracker.expense.dto.MonthlyTotal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * SERVICE: business logic lives here, not in the controller.
 * @Transactional on the class = every public method runs inside a DB transaction.
 */
@Service
@Transactional
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CategoryService categoryService;

    // Constructor injection: Spring sees this constructor and passes in the beans.
    public ExpenseService(ExpenseRepository expenseRepository, CategoryService categoryService) {
        this.expenseRepository = expenseRepository;
        this.categoryService = categoryService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> findAll(LocalDate from, LocalDate to, Long categoryId, Pageable pageable) {
        Specification<Expense> spec = Specification
                .where(ExpenseSpecifications.dateFrom(from))
                .and(ExpenseSpecifications.dateTo(to))
                .and(ExpenseSpecifications.inCategory(categoryId));
        return PageResponse.of(expenseRepository.findAll(spec, pageable), ExpenseResponse::from);
    }

    /** Total spent per month in the given year (months with no expenses are omitted). */
    @Transactional(readOnly = true)
    public List<MonthlyTotal> monthlyTotals(int year) {
        return expenseRepository.findMonthlyTotals(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    @Transactional(readOnly = true)
    public ExpenseResponse findById(Long id) {
        return ExpenseResponse.from(getOrThrow(id));
    }

    public ExpenseResponse create(ExpenseRequest request) {
        Expense expense = new Expense(
                request.title(), request.amount(), request.expenseDate(), request.note(),
                resolveCategory(request.categoryId()));
        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    public ExpenseResponse update(Long id, ExpenseRequest request) {
        Expense expense = getOrThrow(id);
        expense.setTitle(request.title());
        expense.setAmount(request.amount());
        expense.setExpenseDate(request.expenseDate());
        expense.setNote(request.note());
        expense.setCategory(resolveCategory(request.categoryId()));
        // No save() needed! The entity is "managed" inside this transaction, so Hibernate
        // detects the changes (dirty checking) and runs the UPDATE when the method returns.
        return ExpenseResponse.from(expense);
    }

    public void delete(Long id) {
        Expense expense = getOrThrow(id);
        expenseRepository.delete(expense);
    }

    private Category resolveCategory(Long categoryId) {
        return categoryId == null ? null : categoryService.getOrThrow(categoryId);
    }

    private Expense getOrThrow(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense with id " + id + " not found"));
    }
}
