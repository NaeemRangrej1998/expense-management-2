package com.donation.dto.request;

import com.donation.enums.PaymentMethod;
import com.donation.enums.TransactionType;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateTransactionRequest {

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 13, fraction = 2, message = "Invalid amount format")
    private BigDecimal amount;

    private Long incomeCategoryId;

    private Long expenseCategoryId;

    private Long areaId;

    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @Size(max = 200, message = "Reference number too long")
    private String referenceNumber;

    @Size(max = 2000, message = "Description too long")
    private String description;

    // Used for income: who collected it
    private Long collectedById;

    @AssertTrue(message = "Income category is required for income transactions")
    public boolean isIncomeCategoryValid() {
        if (TransactionType.INCOME.equals(type)) {
            return incomeCategoryId != null;
        }
        return true;
    }

    @AssertTrue(message = "Expense category is required for expense transactions")
    public boolean isExpenseCategoryValid() {
        if (TransactionType.EXPENSE.equals(type)) {
            return expenseCategoryId != null;
        }
        return true;
    }
}
