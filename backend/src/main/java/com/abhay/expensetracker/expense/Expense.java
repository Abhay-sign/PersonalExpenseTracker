package com.abhay.expensetracker.expense;

import com.abhay.expensetracker.category.Category;
import com.abhay.expensetracker.user.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * ENTITY: one instance = one row in the "expenses" table.
 * Field names are converted to snake_case columns automatically (expenseDate -> expense_date).
 */
@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB generates the id (BIGSERIAL)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    // Always use BigDecimal for money, never double (floating point rounding errors)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate expenseDate;

    @Column(length = 500)
    private String note;

    // MANY expenses -> ONE category. LAZY = the category row is only loaded when accessed.
    // This side owns the foreign key column (category_id).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    // The owner. Every query in the service filters by it, so users only ever see their own rows.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", updatable = false)
    private User user;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    // JPA needs a no-arg constructor; protected so our own code doesn't use it by accident
    protected Expense() {
    }

    public Expense(String title, BigDecimal amount, LocalDate expenseDate, String note, Category category, User user) {
        this.title = title;
        this.amount = amount;
        this.expenseDate = expenseDate;
        this.note = note;
        this.category = category;
        this.user = user;
    }

    // Lifecycle callbacks: Hibernate calls these right before INSERT / UPDATE
    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public String getNote() { return note; }
    public Category getCategory() { return category; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setTitle(String title) { this.title = title; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
    public void setNote(String note) { this.note = note; }
    public void setCategory(Category category) { this.category = category; }
}
