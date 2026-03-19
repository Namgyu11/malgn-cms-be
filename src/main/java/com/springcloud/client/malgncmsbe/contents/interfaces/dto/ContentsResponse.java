package com.springcloud.client.malgncmsbe.contents.interfaces.dto;

import com.springcloud.client.malgncmsbe.contents.application.result.ContentsResult;

import java.time.LocalDateTime;

public record ContentsResponse(
        Long id,
        String title,
        String description,
        Long viewCount,
        LocalDateTime createdDate,
        String createdBy,
        LocalDateTime lastModifiedDate,
        String lastModifiedBy
) {
    public static ContentsResponse from(ContentsResult result) {
        return new ContentsResponse(
                result.id(),
                result.title(),
                result.description(),
                result.viewCount(),
                result.createdDate(),
                result.createdBy(),
                result.lastModifiedDate(),
                result.lastModifiedBy()
        );
    }
}
