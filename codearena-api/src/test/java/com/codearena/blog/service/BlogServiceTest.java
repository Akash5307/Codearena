package com.codearena.blog.service;

import com.codearena.blog.dto.*;
import com.codearena.blog.entity.BlogPost;
import com.codearena.blog.entity.BlogVote;
import com.codearena.blog.entity.Comment;
import com.codearena.blog.repository.BlogPostRepository;
import com.codearena.blog.repository.BlogVoteRepository;
import com.codearena.blog.repository.CommentRepository;
import com.codearena.common.exception.BusinessException;
import com.codearena.common.exception.ResourceNotFoundException;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlogServiceTest {

    @Mock private BlogPostRepository blogPostRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private BlogVoteRepository blogVoteRepository;
    @Mock private UserRepository userRepository;

    private BlogService blogService;
    private User testUser;

    @BeforeEach
    void setUp() {
        blogService = new BlogService(blogPostRepository, commentRepository, blogVoteRepository, userRepository);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("author");
    }

    private BlogPost createTestPost() {
        BlogPost post = new BlogPost();
        post.setId(1L);
        post.setAuthor(testUser);
        post.setTitle("Test Post");
        post.setContent("Test content");
        post.setUpvotes(0);
        post.setDownvotes(0);
        return post;
    }

    @Test
    void createPost_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(inv -> {
            BlogPost p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        BlogPostDetailResponse response = blogService.createPost(1L,
                new BlogPostCreateRequest("Title", "Content"));

        assertThat(response.title()).isEqualTo("Title");
        assertThat(response.authorUsername()).isEqualTo("author");
        verify(blogPostRepository).save(any(BlogPost.class));
    }

    @Test
    void updatePost_notOwner_throws() {
        BlogPost post = createTestPost();
        when(blogPostRepository.findById(1L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> blogService.updatePost(1L, 99L, new BlogPostUpdateRequest("New", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("You can only edit your own posts");
    }

    @Test
    void updatePost_success() {
        BlogPost post = createTestPost();
        when(blogPostRepository.findById(1L)).thenReturn(Optional.of(post));
        when(blogPostRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(commentRepository.findByBlogPostIdAndParentIsNullOrderByCreatedAtAsc(1L)).thenReturn(List.of());

        BlogPostDetailResponse response = blogService.updatePost(1L, 1L,
                new BlogPostUpdateRequest("Updated Title", null));

        assertThat(response.title()).isEqualTo("Updated Title");
    }

    @Test
    void vote_newUpvote() {
        BlogPost post = createTestPost();
        when(blogPostRepository.findById(1L)).thenReturn(Optional.of(post));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(blogVoteRepository.findByBlogPostIdAndUserId(1L, 1L)).thenReturn(Optional.empty());
        when(blogVoteRepository.save(any())).thenReturn(new BlogVote());

        String result = blogService.vote(1L, 1L, new VoteRequest("UPVOTE"));

        assertThat(result).contains("UPVOTE");
        assertThat(post.getUpvotes()).isEqualTo(1);
        verify(blogVoteRepository).save(any(BlogVote.class));
    }

    @Test
    void vote_toggleOff() {
        BlogPost post = createTestPost();
        post.setUpvotes(1);

        BlogVote existing = new BlogVote();
        existing.setId(1L);
        existing.setVoteType(BlogVote.VoteType.UPVOTE);

        when(blogPostRepository.findById(1L)).thenReturn(Optional.of(post));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(blogVoteRepository.findByBlogPostIdAndUserId(1L, 1L)).thenReturn(Optional.of(existing));

        String result = blogService.vote(1L, 1L, new VoteRequest("UPVOTE"));

        assertThat(result).isEqualTo("Vote removed");
        assertThat(post.getUpvotes()).isEqualTo(0);
        verify(blogVoteRepository).delete(existing);
    }

    @Test
    void vote_switchFromUpToDown() {
        BlogPost post = createTestPost();
        post.setUpvotes(1);

        BlogVote existing = new BlogVote();
        existing.setId(1L);
        existing.setVoteType(BlogVote.VoteType.UPVOTE);

        when(blogPostRepository.findById(1L)).thenReturn(Optional.of(post));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(blogVoteRepository.findByBlogPostIdAndUserId(1L, 1L)).thenReturn(Optional.of(existing));

        String result = blogService.vote(1L, 1L, new VoteRequest("DOWNVOTE"));

        assertThat(result).contains("DOWNVOTE");
        assertThat(post.getUpvotes()).isEqualTo(0);
        assertThat(post.getDownvotes()).isEqualTo(1);
    }

    @Test
    void addComment_success() {
        BlogPost post = createTestPost();
        when(blogPostRepository.findById(1L)).thenReturn(Optional.of(post));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CommentResponse response = blogService.addComment(1L, 1L,
                new CommentCreateRequest("Great post!", null));

        assertThat(response.content()).isEqualTo("Great post!");
        assertThat(response.parentId()).isNull();
    }

    @Test
    void addComment_threaded() {
        BlogPost post = createTestPost();
        Comment parentComment = new Comment();
        parentComment.setId(1L);
        parentComment.setBlogPost(post);
        parentComment.setAuthor(testUser);

        when(blogPostRepository.findById(1L)).thenReturn(Optional.of(post));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(commentRepository.findById(1L)).thenReturn(Optional.of(parentComment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(2L);
            return c;
        });

        CommentResponse response = blogService.addComment(1L, 1L,
                new CommentCreateRequest("Reply", 1L));

        assertThat(response.content()).isEqualTo("Reply");
        assertThat(response.parentId()).isEqualTo(1L);
    }

    @Test
    void getPost_notFound_throws() {
        when(blogPostRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> blogService.getPost(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
