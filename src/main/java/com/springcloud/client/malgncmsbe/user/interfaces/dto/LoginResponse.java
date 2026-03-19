package com.springcloud.client.malgncmsbe.user.interfaces.dto;

public record LoginResponse(String accessToken) {

    public static LoginResponse from(String accessToken) {
        return new LoginResponse(accessToken);
    }
}
