package com.medical.medcore.types;

import org.springframework.data.domain.Page;
import java.util.List;

public record PageableResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PageableResponse<T> from(Page<T> page) {
        return new PageableResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
