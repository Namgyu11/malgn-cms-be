package com.springcloud.client.malgncmsbe.contents.application.result;

import com.springcloud.client.malgncmsbe.contents.domain.Contents;

import java.time.LocalDateTime;

public record ContentsResult(
        Long id,
        String title,
        String description,
        Long viewCount,
        LocalDateTime createdDate,
        String createdBy,
        LocalDateTime lastModifiedDate,
        String lastModifiedBy
) {
    public static ContentsResult from(Contents contents) {
        return new ContentsResult(
                contents.getId(),
                contents.getTitle(),
                contents.getDescription(),
                contents.getViewCount(),
                contents.getCreatedDate(),
                contents.getCreatedBy(),
                contents.getLastModifiedDate(),
                contents.getLastModifiedBy()
        );
    }
}
