package com.springcloud.client.malgncmsbe.contents.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "contents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Contents {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Long viewCount = 0L;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdDate;

    @CreatedBy
    @Column(nullable = false, length = 50, updatable = false)
    private String createdBy;

    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @LastModifiedBy
    @Column(length = 50)
    private String lastModifiedBy;

    @Builder
    public Contents(String title, String description) {
        this.title = title;
        this.description = description;
        this.viewCount = 0L;
    }

    public void update(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public boolean isOwnedBy(String username) {
        return this.createdBy != null && this.createdBy.equals(username);
    }
}
