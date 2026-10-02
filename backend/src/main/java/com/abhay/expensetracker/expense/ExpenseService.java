package com.abhay.expensetracker.expense;

import com.abhay.expensetracker.common.ResourceNotFoundException;
import com.abhay.expensetracker.expense.dto.ExpenseRequest;
import com.abhay.expensetracker.expense.dto.ExpenseResponse;
import org.springframework.data.domain.Sort;
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

    // Constructor injection: Spring sees this constructor and passes in the repository bean.
    public ExpenseService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> findAll(LocalDate from, LocalDate to) {
        List<Expense> expenses;
        if (from == null && to == null) {
            expenses = expenseRepository.findAll(Sort.by(Sort.Direction.DESC, "expenseDate"));
        } else {
            LocalDate start = (from != null) ? from : LocalDate.of(1970, 1, 1);
            LocalDate end = (to != null) ? to : LocalDate.now();
            expenses = expenseRepository.findByExpenseDateBetweenOrderByExpenseDateDesc(start, end);
        }
        return expenses.stream().map(ExpenseResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ExpenseResponse findById(Long id) {
        return ExpenseResponse.from(getOrThrow(id));
    }

    public ExpenseResponse create(ExpenseRequest request) {
        Expense expense = new Expense(
                request.title(), request.amount(), request.expenseDate(), request.note());
        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    public ExpenseResponse update(Long id, ExpenseRequest request) {
        Expense expense = getOrThrow(id);
        expense.setTitle(request.title());
        expense.setAmount(request.amount());
        expense.setExpenseDate(request.expenseDate());
        expense.setNote(request.note());
        // No save() needed! The entity is "managed" inside this transaction, so Hibernate
        // detects the changes (dirty checking) and runs the UPDATE when the method returns.
        return ExpenseResponse.from(expense);
    }

    public void delete(Long id) {
        Expense expense = getOrThrow(id);
        expenseRepository.delete(expense);
    }

    private Expense getOrThrow(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense with id " + id + " not found"));
    }
}
