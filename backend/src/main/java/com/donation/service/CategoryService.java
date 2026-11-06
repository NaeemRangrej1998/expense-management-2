package com.donation.service;

import com.donation.dto.request.CreateCategoryRequest;
import com.donation.entity.ExpenseCategory;
import com.donation.entity.IncomeCategory;
import com.donation.enums.CategoryStatus;
import com.donation.exception.ApiException;
import com.donation.repository.ExpenseCategoryRepository;
import com.donation.repository.IncomeCategoryRepository;
import com.donation.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final IncomeCategoryRepository incomeCategoryRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final AuditService auditService;

    // Income Categories
    @Transactional(readOnly = true)
    public List<IncomeCategory> getAllIncomeCategories() {
        return incomeCategoryRepository.findAllByOrderByNameAsc();
    }

    @Transactional
    public IncomeCategory createIncomeCategory(CreateCategoryRequest request, CustomUserPrincipal principal) {
        if (incomeCategoryRepository.existsByName(request.getName())) {
            throw ApiException.conflict("Income category name already exists");
        }
        IncomeCategory category = new IncomeCategory();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setStatus(request.getStatus() != null ? CategoryStatus.valueOf(request.getStatus().toUpperCase()) : CategoryStatus.ACTIVE);
        
        category = incomeCategoryRepository.save(category);
        auditService.log(principal.getUserId(), "CREATE_INCOME_CATEGORY", "INCOME_CATEGORY", category.getId().toString());
        return category;
    }

    @Transactional
    public IncomeCategory updateIncomeCategory(Long id, CreateCategoryRequest request, CustomUserPrincipal principal) {
        IncomeCategory category = incomeCategoryRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("Income category not found"));
        
        if (incomeCategoryRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw ApiException.conflict("Income category name already exists");
        }
        
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            category.setStatus(CategoryStatus.valueOf(request.getStatus().toUpperCase()));
        }
        
        category = incomeCategoryRepository.save(category);
        auditService.log(principal.getUserId(), "UPDATE_INCOME_CATEGORY", "INCOME_CATEGORY", id.toString());
        return category;
    }

    // Expense Categories
    @Transactional(readOnly = true)
    public List<ExpenseCategory> getAllExpenseCategories() {
        return expenseCategoryRepository.findAllByOrderByNameAsc();
    }

    @Transactional
    public ExpenseCategory createExpenseCategory(CreateCategoryRequest request, CustomUserPrincipal principal) {
        if (expenseCategoryRepository.existsByName(request.getName())) {
            throw ApiException.conflict("Expense category name already exists");
        }
        ExpenseCategory category = new ExpenseCategory();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setStatus(request.getStatus() != null ? CategoryStatus.valueOf(request.getStatus().toUpperCase()) : CategoryStatus.ACTIVE);
        
        category = expenseCategoryRepository.save(category);
        auditService.log(principal.getUserId(), "CREATE_EXPENSE_CATEGORY", "EXPENSE_CATEGORY", category.getId().toString());
        return category;
    }

    @Transactional
    public ExpenseCategory updateExpenseCategory(Long id, CreateCategoryRequest request, CustomUserPrincipal principal) {
        ExpenseCategory category = expenseCategoryRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("Expense category not found"));
        
        if (expenseCategoryRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw ApiException.conflict("Expense category name already exists");
        }
        
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            category.setStatus(CategoryStatus.valueOf(request.getStatus().toUpperCase()));
        }
        
        category = expenseCategoryRepository.save(category);
        auditService.log(principal.getUserId(), "UPDATE_EXPENSE_CATEGORY", "EXPENSE_CATEGORY", id.toString());
        return category;
    }
}
