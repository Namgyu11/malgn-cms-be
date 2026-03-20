package com.springcloud.client.malgncmsbe.contents.infrastructure;

import com.springcloud.client.malgncmsbe.contents.domain.Contents;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

interface ContentsJpaRepository extends JpaRepository<Contents, Long> {

    @Transactional
    @Modifying
    @Query("UPDATE Contents c SET c.viewCount = c.viewCount + 1 WHERE c.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Query("SELECT c FROM Contents c WHERE c.title LIKE %:keyword% OR c.description LIKE %:keyword%")
    Page<Contents> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
