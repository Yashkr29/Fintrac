package com.fintrac.service;

import com.fintrac.dto.TransactionDTO;
import com.fintrac.exception.ResourceNotFoundException;
import com.fintrac.model.*;
import com.fintrac.repository.CategoryRepository;
import com.fintrac.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final AuthService authService;
    private final BudgetService budgetService;
    private final AlertService alertService;
    private final InsightService insightService;

    @Transactional(readOnly = true)
    public List<TransactionDTO> getAllTransactions() {
        Long userId = authService.getCurrentUserId();
        return transactionRepository.findByUserIdOrderByDateDesc(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionsByDateRange(LocalDate startDate, LocalDate endDate) {
        Long userId = authService.getCurrentUserId();
        return transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, startDate, endDate)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionsByType(TransactionType type) {
        Long userId = authService.getCurrentUserId();
        return transactionRepository.findByUserIdAndTypeOrderByDateDesc(userId, type)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TransactionDTO getTransactionById(Long id) {
        Long userId = authService.getCurrentUserId();
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));
        return toDTO(transaction);
    }

    @Transactional
    public TransactionDTO createTransaction(TransactionDTO transactionDTO) {
        User currentUser = authService.getCurrentUser();

        Category category = null;
        if (transactionDTO.getCategoryId() != null) {
            category = categoryRepository.findAccessibleById(transactionDTO.getCategoryId(), currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", transactionDTO.getCategoryId()));
        }

        boolean isEmergency = determineIfEmergency(transactionDTO, category);

        Transaction transaction = Transaction.builder()
                .title(transactionDTO.getTitle())
                .amount(transactionDTO.getAmount())
                .type(transactionDTO.getType())
                .category(category)
                .paymentType(PaymentType.valueOf(transactionDTO.getPaymentType()))
                .merchantName(transactionDTO.getMerchantName())
                .date(transactionDTO.getDate())
                .description(transactionDTO.getDescription())
                .user(currentUser)
                .isEmergency(isEmergency)
                .build();

        Transaction saved = transactionRepository.save(transaction);

        if (transactionDTO.getType() == TransactionType.EXPENSE) {
            budgetService.updateBudgetSpending(currentUser.getId(), transactionDTO.getDate(), transactionDTO.getAmount(), isEmergency);
            alertService.checkBudgetAlerts(currentUser.getId());
            insightService.generateInsights(currentUser.getId());
        }

        return toDTO(saved);
    }

    @Transactional
    public TransactionDTO updateTransaction(Long id, TransactionDTO transactionDTO) {
        Long userId = authService.getCurrentUserId();
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));

        BigDecimal oldAmount = transaction.getAmount();
        boolean oldWasEmergency = transaction.getIsEmergency();
        LocalDate oldDate = transaction.getDate();
        TransactionType oldType = transaction.getType();

        Category category = null;
        if (transactionDTO.getCategoryId() != null) {
            category = categoryRepository.findAccessibleById(transactionDTO.getCategoryId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", transactionDTO.getCategoryId()));
        }

        boolean newIsEmergency = determineIfEmergency(transactionDTO, category);

        transaction.setTitle(transactionDTO.getTitle());
        transaction.setAmount(transactionDTO.getAmount());
        transaction.setType(transactionDTO.getType());
        transaction.setCategory(category);
        transaction.setPaymentType(PaymentType.valueOf(transactionDTO.getPaymentType()));
        transaction.setMerchantName(transactionDTO.getMerchantName());
        transaction.setDate(transactionDTO.getDate());
        transaction.setDescription(transactionDTO.getDescription());
        transaction.setIsEmergency(newIsEmergency);

        Transaction updated = transactionRepository.save(transaction);

        if (oldType == TransactionType.EXPENSE) {
            budgetService.recalculateBudgetOnDelete(userId, oldDate, oldAmount, oldWasEmergency);
        }

        if (transactionDTO.getType() == TransactionType.EXPENSE) {
            budgetService.updateBudgetSpending(userId, transactionDTO.getDate(), transactionDTO.getAmount(), newIsEmergency);
            alertService.checkBudgetAlerts(userId);
        }

        return toDTO(updated);
    }

    @Transactional
    public void deleteTransaction(Long id) {
        Long userId = authService.getCurrentUserId();
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));

        if (transaction.getType() == TransactionType.EXPENSE) {
            budgetService.recalculateBudgetOnDelete(
                    userId,
                    transaction.getDate(),
                    transaction.getAmount(),
                    transaction.getIsEmergency()
            );
        }

        transactionRepository.delete(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionsForMonth(int year, int month) {
        Long userId = authService.getCurrentUserId();
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        return transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, startDate, endDate)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionsForWeek(LocalDate date) {
        Long userId = authService.getCurrentUserId();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        LocalDate startOfWeek = date.with(weekFields.dayOfWeek(), 1);
        LocalDate endOfWeek = date.with(weekFields.dayOfWeek(), 7);
        return transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, startOfWeek, endOfWeek)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private boolean determineIfEmergency(TransactionDTO dto, Category category) {
        if (dto.getIsEmergency() != null && dto.getIsEmergency()) {
            return true;
        }
        if (category != null && category.getName() != null
                && category.getName().toLowerCase().contains("emergency")) {
            return true;
        }
        if (dto.getCategoryName() != null && dto.getCategoryName().toLowerCase().contains("emergency")) {
            return true;
        }
        return false;
    }

    private TransactionDTO toDTO(Transaction transaction) {
        return TransactionDTO.builder()
                .id(transaction.getId())
                .title(transaction.getTitle())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .categoryId(transaction.getCategory() != null ? transaction.getCategory().getId() : null)
                .categoryName(transaction.getCategory() != null ? transaction.getCategory().getName() : null)
                .paymentType(transaction.getPaymentType().name())
                .merchantName(transaction.getMerchantName())
                .date(transaction.getDate())
                .description(transaction.getDescription())
                .isEmergency(transaction.getIsEmergency())
                .build();
    }
}
