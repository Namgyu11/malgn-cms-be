package com.springcloud.client.malgncmsbe.contents.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ContentsRepository {
    Contents save(Contents contents);
    Optional<Contents> findById(Long id);
    Page<Contents> findAll(Pageable pageable);
    Page<Contents> searchByKeyword(String keyword, Pageable pageable);
    void delete(Contents contents);
    void incrementViewCount(Long id);
}
