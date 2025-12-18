package com.employeemanager.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lapozási kérés paraméterek
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {

    private int pageNumber;
    private int pageSize;
    private String sortBy;
    private SortDirection sortDirection;

    public static PageRequest of(int pageNumber, int pageSize) {
        return PageRequest.builder()
            .pageNumber(pageNumber)
            .pageSize(pageSize)
            .sortDirection(SortDirection.ASC)
            .build();
    }

    public static PageRequest of(int pageNumber, int pageSize, String sortBy) {
        return PageRequest.builder()
            .pageNumber(pageNumber)
            .pageSize(pageSize)
            .sortBy(sortBy)
            .sortDirection(SortDirection.ASC)
            .build();
    }

    public int getOffset() {
        return pageNumber * pageSize;
    }

    public enum SortDirection {
        ASC, DESC
    }
}
