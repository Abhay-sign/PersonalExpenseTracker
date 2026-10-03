package com.abhay.expensetracker.expense;

import com.abhay.expensetracker.expense.dto.MonthlyTotal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * REPOSITORY: you write only the interface, Spring Data generates the implementation at startup.
 * JpaSpecificationExecutor adds findAll(Specification, Pageable) for optional, combinable filters.
 */
public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    boolean existsByCategoryId(Long categoryId);

    // Hand-written JPQL (queries entities, not tables). "select new" builds the DTO straight from
    // the result row. Compiles to: SELECT year, month, SUM(amount), COUNT(*) ... GROUP BY year, month
    @Query("""
            select new com.abhay.expensetracker.expense.dto.MonthlyTotal(
                year(e.expenseDate), month(e.expenseDate), sum(e.amount), count(e))
            from Expense e
            where e.expenseDate between :from and :to
            group by year(e.expenseDate), month(e.expenseDate)
            order by year(e.expenseDate), month(e.expenseDate)
            """)
    List<MonthlyTotal> findMonthlyTotals(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
