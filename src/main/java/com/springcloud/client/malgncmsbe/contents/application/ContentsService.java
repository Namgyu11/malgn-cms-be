package com.springcloud.client.malgncmsbe.contents.application;

import com.springcloud.client.malgncmsbe.common.exception.BusinessException;
import com.springcloud.client.malgncmsbe.common.exception.ErrorCode;
import com.springcloud.client.malgncmsbe.contents.application.command.CreateContentsCommand;
import com.springcloud.client.malgncmsbe.contents.application.command.UpdateContentsCommand;
import com.springcloud.client.malgncmsbe.contents.application.result.ContentsResult;
import com.springcloud.client.malgncmsbe.contents.domain.Contents;
import com.springcloud.client.malgncmsbe.contents.domain.ContentsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentsService {

    private final ContentsRepository contentsRepository;

    public Page<ContentsResult> getContents(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return contentsRepository.findAll(pageable).map(ContentsResult::from);
        }
        return contentsRepository.searchByKeyword(keyword.trim(), pageable).map(ContentsResult::from);
    }

    @Transactional
    public ContentsResult getContentsDetail(Long id) {
        contentsRepository.incrementViewCount(id);
        Contents contents = findById(id);
        return ContentsResult.from(contents);
    }

    @Transactional
    public ContentsResult create(CreateContentsCommand command) {
        Contents contents = Contents.builder()
                .title(command.title())
                .description(command.description())
                .build();
        return ContentsResult.from(contentsRepository.save(contents));
    }

    @Transactional
    public ContentsResult update(UpdateContentsCommand command, String username, String role) {
        Contents contents = findById(command.id());
        checkPermission(contents, username, role);
        contents.update(command.title(), command.description());
        contentsRepository.flush();
        contentsRepository.refresh(contents);
        return ContentsResult.from(contents);
    }

    @Transactional
    public void delete(Long id, String username, String role) {
        Contents contents = findById(id);
        checkPermission(contents, username, role);
        contentsRepository.delete(contents);
    }

    private Contents findById(Long id) {
        return contentsRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENTS_NOT_FOUND));
    }

    private void checkPermission(Contents contents, String username, String role) {
        if (!contents.isOwnedBy(username) && !"ADMIN".equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
