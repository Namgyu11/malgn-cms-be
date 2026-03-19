package com.springcloud.client.malgncmsbe.user.application.command;

public record LoginCommand(
        String username,
        String password
) {}
