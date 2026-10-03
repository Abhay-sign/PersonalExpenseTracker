package com.abhay.expensetracker.expense;

import com.abhay.expensetracker.common.PageResponse;
import com.abhay.expensetracker.expense.dto.ExpenseRequest;
import com.abhay.expensetracker.expense.dto.ExpenseResponse;
import com.abhay.expensetracker.expense.dto.MonthlyTotal;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

/**
 * CONTROLLER: only handles HTTP (URLs, status codes, JSON). Delegates all logic to the service.
 * @RestController = every method's return value is converted to JSON (by Jackson).
 */
@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    // GET /api/expenses?page=0&size=20&sort=amount,desc&from=2026-09-01&to=2026-09-30&categoryId=1
    // All params are optional. Pageable is filled from page/size/sort query params automatically.
    @GetMapping
    public PageResponse<ExpenseResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 20, sort = "expenseDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return expenseService.findAll(from, to, categoryId, pageable);
    }

    // GET /api/expenses/summary/monthly?year=2026 (defaults to the current year)
    @GetMapping("/summary/monthly")
    public List<MonthlyTotal> monthlyTotals(@RequestParam(required = false) Integer year) {
        return expenseService.monthlyTotals(year != null ? year : Year.now().getValue());
    }

    @GetMapping("/{id}")
    public ExpenseResponse get(@PathVariable Long id) {
        return expenseService.findById(id);
    }

    // @Valid triggers the annotations in ExpenseRequest; failures go to GlobalExceptionHandler
    @PostMapping
    public ResponseEntity<ExpenseResponse> create(@Valid @RequestBody ExpenseRequest request) {
        ExpenseResponse created = expenseService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created); // 201 + Location header
    }

    @PutMapping("/{id}")
    public ExpenseResponse update(@PathVariable Long id, @Valid @RequestBody ExpenseRequest request) {
        return expenseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    public void delete(@PathVariable Long id) {
        expenseService.delete(id);
    }
}
