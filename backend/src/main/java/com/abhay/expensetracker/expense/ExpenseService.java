package com.abhay.expensetracker.expense;

import com.abhay.expensetracker.category.Category;
import com.abhay.expensetracker.category.CategoryService;
import com.abhay.expensetracker.common.PageResponse;
import com.abhay.expensetracker.common.ResourceNotFoundException;
import com.abhay.expensetracker.expense.dto.ExpenseRequest;
import com.abhay.expensetracker.expense.dto.ExpenseResponse;
import com.abhay.expensetracker.expense.dto.MonthlyTotal;
import com.abhay.expensetracker.user.UserRepository;
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
    private final UserRepository userRepository;

    // Constructor injection: Spring sees this constructor and passes in the beans.
    public ExpenseService(ExpenseRepository expenseRepository, CategoryService categoryService,
                          UserRepository userRepository) {
        this.expenseRepository = expenseRepository;
        this.categoryService = categoryService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> findAll(Long userId, LocalDate from, LocalDate to, Long categoryId, Pageable pageable) {
        Specification<Expense> spec = Specification
                .where(ExpenseSpecifications.ownedBy(userId))
                .and(ExpenseSpecifications.dateFrom(from))
                .and(ExpenseSpecifications.dateTo(to))
                .and(ExpenseSpecifications.inCategory(categoryId));
        return PageResponse.of(expenseRepository.findAll(spec, pageable), ExpenseResponse::from);
    }

    /** Total spent per month in the given year (months with no expenses are omitted). */
    @Transactional(readOnly = true)
    public List<MonthlyTotal> monthlyTotals(Long userId, int year) {
        return expenseRepository.findMonthlyTotals(userId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    @Transactional(readOnly = true)
    public ExpenseResponse findById(Long userId, Long id) {
        return ExpenseResponse.from(getOrThrow(userId, id));
    }

    public ExpenseResponse create(Long userId, ExpenseRequest request) {
        Expense expense = new Expense(
                request.title(), request.amount(), request.expenseDate(), request.note(),
                resolveCategory(request.categoryId()),
                userRepository.getReferenceById(userId)); // proxy by id, no SELECT needed
        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    public ExpenseResponse update(Long userId, Long id, ExpenseRequest request) {
        Expense expense = getOrThrow(userId, id);
        expense.setTitle(request.title());
        expense.setAmount(request.amount());
        expense.setExpenseDate(request.expenseDate());
        expense.setNote(request.note());
        expense.setCategory(resolveCategory(request.categoryId()));
        // No save() needed! The entity is "managed" inside this transaction, so Hibernate
        // detects the changes (dirty checking) and runs the UPDATE when the method returns.
        return ExpenseResponse.from(expense);
    }

    public void delete(Long userId, Long id) {
        Expense expense = getOrThrow(userId, id);
        expenseRepository.delete(expense);
    }

    private Category resolveCategory(Long categoryId) {
        return categoryId == null ? null : categoryService.getOrThrow(categoryId);
    }

    // Someone else's expense looks exactly like a missing one (404), so ids can't be probed.
    private Expense getOrThrow(Long userId, Long id) {
        return expenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense with id " + id + " not found"));
    }
}
