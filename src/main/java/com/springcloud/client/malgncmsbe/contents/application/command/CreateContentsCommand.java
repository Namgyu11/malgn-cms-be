package com.springcloud.client.malgncmsbe.contents.application.command;

public record CreateContentsCommand(
        String title,
        String description
) {}
