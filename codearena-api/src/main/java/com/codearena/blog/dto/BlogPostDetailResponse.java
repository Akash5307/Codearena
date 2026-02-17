package com.codearena.blog.dto;

import com.codearena.blog.entity.BlogPost;

import java.time.LocalDateTime;
import java.util.List;

public record BlogPostDetailResponse(
        Long id,
        String title,
        String content,
        String authorUsername,
        int upvotes,
        int downvotes,
        List<CommentResponse> comments,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static BlogPostDetailResponse from(BlogPost post, List<CommentResponse> comments) {
        return new BlogPostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor().getUsername(),
                post.getUpvotes(),
                post.getDownvotes(),
                comments,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
