package com.fintrac.dto;

import com.fintrac.model.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    private Long categoryId;
    private String categoryName;

    @NotNull(message = "Payment type is required")
    private String paymentType;

    private String merchantName;

    @NotNull(message = "Date is required")
    private LocalDate date;

    private String description;
    private Boolean isEmergency;
}
