package com.springcloud.client.malgncmsbe.contents.application.command;

public record UpdateContentsCommand(
        Long id,
        String title,
        String description
) {}
