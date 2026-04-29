package com.learn.bankingapi.dto.response.transaction;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Generic paginated response")
public record PageResponse<T>(
        @Schema(description = "Page content")
        List<T> content,
        @Schema(description = "Total number of elements", example = "100")
        long totalElements,
        @Schema(description = "Total number of pages", example = "5")
        int totalPages,
        @Schema(description = "Current page number (zero-based)", example = "0")
        int page,
        @Schema(description = "Number of elements per page", example = "20")
        int size
) {}