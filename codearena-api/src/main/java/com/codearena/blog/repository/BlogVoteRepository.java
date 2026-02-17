package com.codearena.blog.repository;

import com.codearena.blog.entity.BlogVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlogVoteRepository extends JpaRepository<BlogVote, Long> {

    Optional<BlogVote> findByBlogPostIdAndUserId(Long blogPostId, Long userId);
}
