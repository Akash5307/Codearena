package com.codearena.problem.dto;

import com.codearena.problem.entity.Tag;

public record TagResponse(Long id, String name) {
    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }
}
