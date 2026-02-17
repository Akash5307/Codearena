package com.codearena.blog.dto;

public record BlogPostUpdateRequest(
        String title,
        String content
) {}
