package com.donation.controller;

import com.donation.dto.request.CreateCategoryRequest;
import com.donation.entity.ExpenseCategory;
import com.donation.entity.IncomeCategory;
import com.donation.enums.CategoryStatus;
import com.donation.repository.ExpenseCategoryRepository;
import com.donation.repository.IncomeCategoryRepository;
import com.donation.security.CustomUserPrincipal;
import com.donation.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final IncomeCategoryRepository incomeCategoryRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;

    @GetMapping("/income")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<IncomeCategory>> getIncomeCategories() {
        return ResponseEntity.ok(incomeCategoryRepository.findByStatusOrderByNameAsc(CategoryStatus.ACTIVE));
    }

    @GetMapping("/income/all")
    @PreAuthorize("hasAuthority('roles.view')") // Using a generic admin permission for full list
    public ResponseEntity<List<IncomeCategory>> getAllIncomeCategories() {
        return ResponseEntity.ok(categoryService.getAllIncomeCategories());
    }

    @PostMapping("/income")
    @PreAuthorize("hasAuthority('roles.create')")
    public ResponseEntity<IncomeCategory> createIncomeCategory(
        @Valid @RequestBody CreateCategoryRequest request,
        @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.status(201).body(categoryService.createIncomeCategory(request, principal));
    }

    @PutMapping("/income/{id}")
    @PreAuthorize("hasAuthority('roles.update')")
    public ResponseEntity<IncomeCategory> updateIncomeCategory(
        @PathVariable Long id,
        @Valid @RequestBody CreateCategoryRequest request,
        @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(categoryService.updateIncomeCategory(id, request, principal));
    }

    @GetMapping("/expense")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ExpenseCategory>> getExpenseCategories() {
        return ResponseEntity.ok(expenseCategoryRepository.findByStatusOrderByNameAsc(CategoryStatus.ACTIVE));
    }

    @GetMapping("/expense/all")
    @PreAuthorize("hasAuthority('roles.view')")
    public ResponseEntity<List<ExpenseCategory>> getAllExpenseCategories() {
        return ResponseEntity.ok(categoryService.getAllExpenseCategories());
    }

    @PostMapping("/expense")
    @PreAuthorize("hasAuthority('roles.create')")
    public ResponseEntity<ExpenseCategory> createExpenseCategory(
        @Valid @RequestBody CreateCategoryRequest request,
        @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.status(201).body(categoryService.createExpenseCategory(request, principal));
    }

    @PutMapping("/expense/{id}")
    @PreAuthorize("hasAuthority('roles.update')")
    public ResponseEntity<ExpenseCategory> updateExpenseCategory(
        @PathVariable Long id,
        @Valid @RequestBody CreateCategoryRequest request,
        @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(categoryService.updateExpenseCategory(id, request, principal));
    }
}
