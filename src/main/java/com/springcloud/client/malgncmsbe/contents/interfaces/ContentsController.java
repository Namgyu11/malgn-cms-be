package com.springcloud.client.malgncmsbe.contents.interfaces;

import com.springcloud.client.malgncmsbe.common.response.ApiResponse;
import com.springcloud.client.malgncmsbe.common.response.PageResponse;
import com.springcloud.client.malgncmsbe.contents.application.ContentsService;
import com.springcloud.client.malgncmsbe.contents.application.command.CreateContentsCommand;
import com.springcloud.client.malgncmsbe.contents.application.command.UpdateContentsCommand;
import com.springcloud.client.malgncmsbe.contents.interfaces.dto.ContentsCreateRequest;
import com.springcloud.client.malgncmsbe.contents.interfaces.dto.ContentsResponse;
import com.springcloud.client.malgncmsbe.contents.interfaces.dto.ContentsUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class ContentsController {

    private final ContentsService contentsService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ContentsResponse>>> getContents(
            @PageableDefault(size = 10, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<ContentsResponse> response = new PageResponse<>(
                contentsService.getContents(pageable).map(ContentsResponse::from)
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContentsResponse>> getContentsDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(ContentsResponse.from(contentsService.getContentsDetail(id))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ContentsResponse>> create(
            @RequestBody @Valid ContentsCreateRequest request,
            Authentication authentication) {
        ContentsResponse response = ContentsResponse.from(
                contentsService.create(new CreateContentsCommand(request.title(), request.description()))
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ContentsResponse>> update(
            @PathVariable Long id,
            @RequestBody @Valid ContentsUpdateRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        String role = extractRole(authentication);
        ContentsResponse response = ContentsResponse.from(
                contentsService.update(new UpdateContentsCommand(id, request.title(), request.description()), username, role)
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        String role = extractRole(authentication);
        contentsService.delete(id, username, role);
        return ResponseEntity.ok(ApiResponse.success());
    }

    private String extractRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse("USER");
    }
}
