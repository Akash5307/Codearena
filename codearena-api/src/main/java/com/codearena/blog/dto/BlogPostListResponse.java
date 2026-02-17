package com.codearena.blog.dto;

import com.codearena.blog.entity.BlogPost;

import java.time.LocalDateTime;

public record BlogPostListResponse(
        Long id,
        String title,
        String authorUsername,
        int upvotes,
        int downvotes,
        int commentCount,
        LocalDateTime createdAt
) {
    public static BlogPostListResponse from(BlogPost post) {
        return new BlogPostListResponse(
                post.getId(),
                post.getTitle(),
                post.getAuthor().getUsername(),
                post.getUpvotes(),
                post.getDownvotes(),
                post.getComments() != null ? post.getComments().size() : 0,
                post.getCreatedAt()
        );
    }
}
