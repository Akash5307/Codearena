package com.codearena.blog.repository;

import com.codearena.blog.entity.BlogPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    Page<BlogPost> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<BlogPost> findByAuthorIdOrderByCreatedAtDesc(Long authorId, Pageable pageable);
}
