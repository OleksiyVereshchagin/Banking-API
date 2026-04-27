package com.learn.bankingapi.dto.filter;

import com.learn.bankingapi.enums.Currency;
import com.learn.bankingapi.enums.TransactionStatus;
import com.learn.bankingapi.enums.TransactionType;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TransactionFilter(
        @Positive
        BigDecimal amountMin,

        @Positive
        BigDecimal amountMax,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate createdAtFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate createdAtTo,

        List<TransactionType> types,

        List<TransactionStatus> statuses,

        List<Currency> currencies
) {}
