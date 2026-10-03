package com.abhay.expensetracker.category;

import com.abhay.expensetracker.category.dto.CategoryRequest;
import com.abhay.expensetracker.category.dto.CategoryResponse;
import com.abhay.expensetracker.common.ConflictException;
import com.abhay.expensetracker.common.ResourceNotFoundException;
import com.abhay.expensetracker.expense.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;

    public CategoryService(CategoryRepository categoryRepository, ExpenseRepository expenseRepository) {
        this.categoryRepository = categoryRepository;
        this.expenseRepository = expenseRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAllByOrderByNameAsc().stream().map(CategoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return CategoryResponse.from(getOrThrow(id));
    }

    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Category '" + name + "' already exists");
        }
        return CategoryResponse.from(categoryRepository.save(new Category(name)));
    }

    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getOrThrow(id);
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Category '" + name + "' already exists");
        }
        category.setName(name);
        return CategoryResponse.from(category);
    }

    public void delete(Long id) {
        Category category = getOrThrow(id);
        if (expenseRepository.existsByCategoryId(id)) {
            throw new ConflictException("Category '" + category.getName() + "' still has expenses");
        }
        categoryRepository.delete(category);
    }

    public Category getOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category with id " + id + " not found"));
    }
}
