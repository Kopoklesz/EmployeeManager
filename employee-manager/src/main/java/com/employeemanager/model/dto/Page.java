package com.employeemanager.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Lapozható eredmény
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Page<T> {

    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static <T> Page<T> of(List<T> content, PageRequest pageRequest, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / pageRequest.getPageSize());
        return Page.<T>builder()
            .content(content)
            .pageNumber(pageRequest.getPageNumber())
            .pageSize(pageRequest.getPageSize())
            .totalElements(totalElements)
            .totalPages(totalPages)
            .first(pageRequest.getPageNumber() == 0)
            .last(pageRequest.getPageNumber() >= totalPages - 1)
            .build();
    }

    public boolean hasNext() {
        return !last;
    }

    public boolean hasPrevious() {
        return !first;
    }
}
