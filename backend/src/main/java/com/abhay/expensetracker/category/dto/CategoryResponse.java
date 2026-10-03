package com.abhay.expensetracker.category.dto;

import com.abhay.expensetracker.category.Category;

public record CategoryResponse(Long id, String name) {
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getName());
    }
}
