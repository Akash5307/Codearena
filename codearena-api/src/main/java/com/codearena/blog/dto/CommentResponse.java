package com.codearena.blog.dto;

import com.codearena.blog.entity.Comment;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
        Long id,
        String authorUsername,
        String content,
        Long parentId,
        List<CommentResponse> replies,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CommentResponse from(Comment comment) {
        List<CommentResponse> replyResponses = comment.getReplies() != null
                ? comment.getReplies().stream().map(CommentResponse::from).toList()
                : List.of();

        return new CommentResponse(
                comment.getId(),
                comment.getAuthor().getUsername(),
                comment.getContent(),
                comment.getParent() != null ? comment.getParent().getId() : null,
                replyResponses,
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
