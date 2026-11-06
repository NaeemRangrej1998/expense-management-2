package com.donation.service;

import com.donation.dto.request.CreateTransactionRequest;
import com.donation.dto.request.ApprovalRequest;
import com.donation.dto.response.PageResponse;
import com.donation.dto.response.TransactionResponse;
import com.donation.entity.*;
import com.donation.enums.CategoryStatus;
import com.donation.enums.*;
import com.donation.exception.ApiException;
import com.donation.repository.*;
import com.donation.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionApprovalRepository approvalRepository;
    private final UserRepository userRepository;
    private final AreaRepository areaRepository;
    private final IncomeCategoryRepository incomeCategoryRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final AuditService auditService;
    private final AttachmentService attachmentService;

    @Transactional(rollbackFor = Exception.class)
    public TransactionResponse createTransaction(
        CreateTransactionRequest request,
        MultipartFile attachment,
        CustomUserPrincipal principal
    ) throws IOException {
        User currentUser = getUser(principal.getUserId());

        // Validate permissions
        if (TransactionType.INCOME.equals(request.getType())) {
            if (!principal.hasPermission("income.create")) {
                throw new AccessDeniedException("No permission to create income");
            }
        } else {
            if (!principal.hasPermission("expense.create")) {
                throw new AccessDeniedException("No permission to create expense");
            }
        }

        // Determine creator (collected_by for income)
        User creator = currentUser;
        if (request.getCollectedById() != null && !request.getCollectedById().equals(principal.getUserId())) {
            if (!principal.isSuperAdmin()) {
                throw new AccessDeniedException("Cannot create transactions on behalf of others");
            }
            creator = getUser(request.getCollectedById());
        }

        // Validate and authorize area
        Area area = null;
        if (request.getAreaId() != null) {
            area = areaRepository.findById(request.getAreaId())
                .orElseThrow(() -> ApiException.notFound("Area not found"));
            if (!AreaStatus.ACTIVE.equals(area.getStatus())) {
                throw ApiException.badRequest("Area is not active");
            }
            if (!principal.isSuperAdmin()) {
                // Check user is assigned to this area — capture final ref for lambda
                final Area finalArea = area;
                boolean hasAreaAccess = currentUser.getAreas().stream()
                    .anyMatch(a -> a.getId().equals(finalArea.getId()));
                if (!hasAreaAccess) {
                    throw ApiException.forbidden("No access to this area");
                }
            }
        }

        // Validate categories
        IncomeCategory incomeCategory = null;
        ExpenseCategory expenseCategory = null;
        if (TransactionType.INCOME.equals(request.getType())) {
            incomeCategory = incomeCategoryRepository.findById(request.getIncomeCategoryId())
                .orElseThrow(() -> ApiException.notFound("Income category not found"));
            if (!CategoryStatus.ACTIVE.equals(incomeCategory.getStatus())) {
                throw ApiException.badRequest("Income category is not active");
            }
        } else {
            expenseCategory = expenseCategoryRepository.findById(request.getExpenseCategoryId())
                .orElseThrow(() -> ApiException.notFound("Expense category not found"));
            if (!CategoryStatus.ACTIVE.equals(expenseCategory.getStatus())) {
                throw ApiException.badRequest("Expense category is not active");
            }
        }

        // Generate transaction number
        String txnNumber = generateTransactionNumber();

        // Build transaction
        Transaction transaction = Transaction.builder()
            .transactionNumber(txnNumber)
            .type(request.getType())
            .amount(request.getAmount())
            .incomeCategory(incomeCategory)
            .expenseCategory(expenseCategory)
            .area(area)
            .createdBy(creator)
            .transactionDate(request.getTransactionDate())
            .paymentMethod(request.getPaymentMethod())
            .referenceNumber(request.getReferenceNumber())
            .description(request.getDescription())
            .status(TransactionStatus.PENDING)
            .build();

        // Expenses created by Super Admin are auto-approved
        if (TransactionType.EXPENSE.equals(request.getType()) && principal.isSuperAdmin()) {
            transaction.setStatus(TransactionStatus.APPROVED);
            transaction.setApprovedBy(currentUser);
            transaction.setApprovedAt(LocalDateTime.now());

            TransactionApproval approval = TransactionApproval.builder()
                .transaction(transaction)
                .action(ApprovalAction.APPROVED)
                .performedBy(currentUser)
                .comments("Auto-approved by Super Admin creator")
                .build();
            transaction = transactionRepository.save(transaction);
            approval.setTransaction(transaction);
            approvalRepository.save(approval);
        } else {
            transaction = transactionRepository.save(transaction);
        }

        auditService.log(principal.getUserId(), "CREATE_TRANSACTION", "TRANSACTION",
            transaction.getId().toString());

        attachmentService.uploadAttachment(transaction.getId(), attachment, principal);
        return TransactionResponse.from(transaction);
    }

    @Transactional
    public TransactionResponse approveTransaction(Long id, ApprovalRequest request, CustomUserPrincipal principal) {
        if (!principal.isSuperAdmin() && !principal.hasPermission("income.approve")) {
            throw new AccessDeniedException("No permission to approve transactions");
        }

        Transaction transaction = transactionRepository.findByIdWithLock(id)
            .orElseThrow(() -> ApiException.notFound("Transaction not found"));

        if (!TransactionType.INCOME.equals(transaction.getType()) && !principal.isSuperAdmin()) {
            throw new AccessDeniedException("No permission to approve transactions");
        }

        if (!TransactionStatus.PENDING.equals(transaction.getStatus())) {
            throw ApiException.conflict("Transaction is not in pending status (current: " + transaction.getStatus() + ")");
        }

        User approver = getUser(principal.getUserId());
        transaction.setStatus(TransactionStatus.APPROVED);
        transaction.setApprovedBy(approver);
        transaction.setApprovedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        TransactionApproval approval = TransactionApproval.builder()
            .transaction(transaction)
            .action(ApprovalAction.APPROVED)
            .performedBy(approver)
            .comments(request.getComments())
            .build();
        approvalRepository.save(approval);

        auditService.log(principal.getUserId(), "APPROVE_TRANSACTION", "TRANSACTION", id.toString());
        return TransactionResponse.from(transaction);
    }

    @Transactional
    public TransactionResponse rejectTransaction(Long id, ApprovalRequest request, CustomUserPrincipal principal) {
        if (!principal.isSuperAdmin() && !principal.hasPermission("income.reject")) {
            throw new AccessDeniedException("No permission to reject transactions");
        }

        Transaction transaction = transactionRepository.findByIdWithLock(id)
            .orElseThrow(() -> ApiException.notFound("Transaction not found"));

        if (!TransactionType.INCOME.equals(transaction.getType()) && !principal.isSuperAdmin()) {
            throw new AccessDeniedException("No permission to reject transactions");
        }

        if (!TransactionStatus.PENDING.equals(transaction.getStatus())) {
            throw ApiException.conflict("Transaction is not in pending status");
        }

        User reviewer = getUser(principal.getUserId());
        transaction.setStatus(TransactionStatus.REJECTED);
        transaction.setApprovedBy(reviewer);
        transaction.setApprovedAt(LocalDateTime.now());
        transaction.setRejectionReason(request.getComments());
        transactionRepository.save(transaction);

        TransactionApproval approval = TransactionApproval.builder()
            .transaction(transaction)
            .action(ApprovalAction.REJECTED)
            .performedBy(reviewer)
            .comments(request.getComments())
            .build();
        approvalRepository.save(approval);

        auditService.log(principal.getUserId(), "REJECT_TRANSACTION", "TRANSACTION", id.toString());
        return TransactionResponse.from(transaction);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long id, CreateTransactionRequest request, CustomUserPrincipal principal) {
        Transaction transaction = transactionRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("Transaction not found"));

        // Validate permissions
        if (TransactionType.INCOME.equals(transaction.getType())) {
            if (!principal.hasPermission("income.update")) {
                throw new AccessDeniedException("No permission to update income");
            }
        } else {
            if (!principal.hasPermission("expense.update")) {
                throw new AccessDeniedException("No permission to update expense");
            }
        }

        // Only PENDING transactions can be updated
        if (!TransactionStatus.PENDING.equals(transaction.getStatus())) {
            throw ApiException.conflict("Only pending transactions can be updated");
        }

        // Validate Area access
        validateAreaAccess(transaction, principal);

        // Update basic fields
        transaction.setAmount(request.getAmount());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setPaymentMethod(request.getPaymentMethod());
        transaction.setReferenceNumber(request.getReferenceNumber());
        transaction.setDescription(request.getDescription());

        // Update Area if provided
        if (request.getAreaId() != null) {
            Area area = areaRepository.findById(request.getAreaId())
                .orElseThrow(() -> ApiException.notFound("Area not found"));
            if (!AreaStatus.ACTIVE.equals(area.getStatus())) {
                throw ApiException.badRequest("Area is not active");
            }
            // Check access to new area
            if (!principal.isSuperAdmin()) {
                User currentUser = getUser(principal.getUserId());
                boolean hasAreaAccess = currentUser.getAreas().stream()
                    .anyMatch(a -> a.getId().equals(area.getId()));
                if (!hasAreaAccess) {
                    throw ApiException.forbidden("No access to this area");
                }
            }
            transaction.setArea(area);
        }

        // Update Category
        if (TransactionType.INCOME.equals(transaction.getType())) {
            IncomeCategory incomeCategory = incomeCategoryRepository.findById(request.getIncomeCategoryId())
                .orElseThrow(() -> ApiException.notFound("Income category not found"));
            if (!CategoryStatus.ACTIVE.equals(incomeCategory.getStatus())) {
                throw ApiException.badRequest("Income category is not active");
            }
            transaction.setIncomeCategory(incomeCategory);
        } else {
            ExpenseCategory expenseCategory = expenseCategoryRepository.findById(request.getExpenseCategoryId())
                .orElseThrow(() -> ApiException.notFound("Expense category not found"));
            if (!CategoryStatus.ACTIVE.equals(expenseCategory.getStatus())) {
                throw ApiException.badRequest("Expense category is not active");
            }
            transaction.setExpenseCategory(expenseCategory);
        }

        transaction = transactionRepository.save(transaction);
        auditService.log(principal.getUserId(), "UPDATE_TRANSACTION", "TRANSACTION", id.toString());
        return TransactionResponse.from(transaction);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(Long id, CustomUserPrincipal principal) {
        Transaction transaction = transactionRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("Transaction not found"));

        // Area access check
        validateAreaAccess(transaction, principal);
        return TransactionResponse.from(transaction);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getTransactions(
        TransactionType type,
        String search,
        TransactionStatus status,
        LocalDate startDate,
        LocalDate endDate,
        Long createdById,
        int page, int size,
        CustomUserPrincipal principal
    ) {
        List<Long> areaIds = getAuthorizedAreaIds(principal);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Transaction> result = transactionRepository.findAllWithFilters(
            type, areaIds, status, startDate, endDate, search, createdById, pageable
        );

        Page<TransactionResponse> mapped = result.map(TransactionResponse::from);
        return PageResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getHistory(
        TransactionType type,
        String search,
        LocalDate startDate,
        LocalDate endDate,
        int page, int size,
        CustomUserPrincipal principal
    ) {
        List<Long> areaIds = getAuthorizedAreaIds(principal);
        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<Transaction> result = transactionRepository.findHistory(
            type, areaIds, startDate, endDate, search, pageable
        );
        return PageResponse.from(result.map(TransactionResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getDailyIncomeReport(
        LocalDate date, Long userId, TransactionStatus status,
        int page, int size, CustomUserPrincipal principal
    ) {
        List<Long> areaIds = getAuthorizedAreaIds(principal);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Transaction> result = transactionRepository.findDailyIncomeReport(
            date, areaIds, userId, status, pageable
        );
        return PageResponse.from(result.map(TransactionResponse::from));
    }

    @Transactional(readOnly = true)
    public String exportTransactionsToCsv(
        TransactionType type,
        TransactionStatus status,
        LocalDate startDate,
        LocalDate endDate,
        CustomUserPrincipal principal
    ) {
        if (!principal.hasPermission("reports.view")) {
            throw new AccessDeniedException("No permission to view reports");
        }

        List<Long> areaIds = getAuthorizedAreaIds(principal);
        List<Transaction> transactions = transactionRepository.findAllWithFiltersList(
            type, areaIds, status, startDate, endDate
        );

        StringBuilder csv = new StringBuilder();
        csv.append("Transaction Number,Date,Type,Amount,Category,Area,Payment Method,Status,Reference,Created By\n");

        for (Transaction txn : transactions) {
            String category = txn.getType() == TransactionType.INCOME 
                ? (txn.getIncomeCategory() != null ? txn.getIncomeCategory().getName() : "")
                : (txn.getExpenseCategory() != null ? txn.getExpenseCategory().getName() : "");
            
            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                txn.getTransactionNumber(),
                txn.getTransactionDate(),
                txn.getType(),
                txn.getAmount(),
                category,
                txn.getArea() != null ? txn.getArea().getName() : "",
                txn.getPaymentMethod(),
                txn.getStatus(),
                txn.getReferenceNumber() != null ? txn.getReferenceNumber() : "",
                txn.getCreatedBy().getName()
            ));
        }

        auditService.log(principal.getUserId(), "EXPORT_TRANSACTIONS", "REPORT", "CSV");
        return csv.toString();
    }

    private List<Long> getAuthorizedAreaIds(CustomUserPrincipal principal) {
        if (principal.isSuperAdmin()) {
            return null; // null = all areas
        }
        User user = userRepository.findById(principal.getUserId()).orElseThrow();
        List<Long> ids = user.getAreas().stream().map(Area::getId).collect(Collectors.toList());
        return ids.isEmpty() ? List.of(-1L) : ids; // -1L ensures empty result if no areas assigned
    }

    private String generateTransactionNumber() {
        int year = Year.now().getValue();
        long count = transactionRepository.countByYear(year);
        return String.format("TXN-%d-%08d", year, count);
    }

    private User getUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
    }

    private void validateAreaAccess(Transaction transaction, CustomUserPrincipal principal) {
        if (principal.isSuperAdmin() || transaction.getArea() == null) return;
        User user = userRepository.findById(principal.getUserId()).orElseThrow();
        boolean hasAccess = user.getAreas().stream()
            .anyMatch(a -> a.getId().equals(transaction.getArea().getId()));
        if (!hasAccess && !transaction.getCreatedBy().getId().equals(principal.getUserId())) {
            throw ApiException.forbidden("No access to this transaction");
        }
    }
}
