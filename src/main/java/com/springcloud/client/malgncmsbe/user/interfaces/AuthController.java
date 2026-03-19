package com.springcloud.client.malgncmsbe.user.interfaces;

import com.springcloud.client.malgncmsbe.common.response.ApiResponse;
import com.springcloud.client.malgncmsbe.user.application.AuthService;
import com.springcloud.client.malgncmsbe.user.application.command.LoginCommand;
import com.springcloud.client.malgncmsbe.user.application.command.SignupCommand;
import com.springcloud.client.malgncmsbe.user.application.result.TokenResult;
import com.springcloud.client.malgncmsbe.user.interfaces.dto.LoginRequest;
import com.springcloud.client.malgncmsbe.user.interfaces.dto.LoginResponse;
import com.springcloud.client.malgncmsbe.user.interfaces.dto.SignupRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody @Valid LoginRequest request) {
        TokenResult result = authService.login(new LoginCommand(request.username(), request.password()));
        return ResponseEntity.ok(ApiResponse.success(LoginResponse.from(result.accessToken())));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(
            @RequestBody @Valid SignupRequest request) {
        authService.signup(new SignupCommand(request.username(), request.password()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success());
    }
}
