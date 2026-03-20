package com.springcloud.client.malgncmsbe.contents.infrastructure;

import com.springcloud.client.malgncmsbe.contents.domain.Contents;
import com.springcloud.client.malgncmsbe.contents.domain.ContentsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ContentsRepositoryImpl implements ContentsRepository {

    private final ContentsJpaRepository contentsJpaRepository;

    @Override
    public Contents save(Contents contents) {
        return contentsJpaRepository.save(contents);
    }

    @Override
    public Optional<Contents> findById(Long id) {
        return contentsJpaRepository.findById(id);
    }

    @Override
    public Page<Contents> findAll(Pageable pageable) {
        return contentsJpaRepository.findAll(pageable);
    }

    @Override
    public Page<Contents> searchByKeyword(String keyword, Pageable pageable) {
        return contentsJpaRepository.searchByKeyword(keyword, pageable);
    }

    @Override
    public void delete(Contents contents) {
        contentsJpaRepository.delete(contents);
    }

    @Override
    public void incrementViewCount(Long id) {
        contentsJpaRepository.incrementViewCount(id);
    }
}
