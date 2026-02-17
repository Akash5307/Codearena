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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BlogService {

    private final BlogPostRepository blogPostRepository;
    private final CommentRepository commentRepository;
    private final BlogVoteRepository blogVoteRepository;
    private final UserRepository userRepository;

    public BlogService(BlogPostRepository blogPostRepository,
                       CommentRepository commentRepository,
                       BlogVoteRepository blogVoteRepository,
                       UserRepository userRepository) {
        this.blogPostRepository = blogPostRepository;
        this.commentRepository = commentRepository;
        this.blogVoteRepository = blogVoteRepository;
        this.userRepository = userRepository;
    }

    public Page<BlogPostListResponse> listPosts(Pageable pageable) {
        return blogPostRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(BlogPostListResponse::from);
    }

    public BlogPostDetailResponse getPost(Long id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BlogPost", "id", id));

        List<CommentResponse> comments = commentRepository
                .findByBlogPostIdAndParentIsNullOrderByCreatedAtAsc(id)
                .stream()
                .map(CommentResponse::from)
                .toList();

        return BlogPostDetailResponse.from(post, comments);
    }

    @Transactional
    public BlogPostDetailResponse createPost(Long authorId, BlogPostCreateRequest request) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", authorId));

        BlogPost post = new BlogPost();
        post.setAuthor(author);
        post.setTitle(request.title());
        post.setContent(request.content());
        post = blogPostRepository.save(post);

        return BlogPostDetailResponse.from(post, List.of());
    }

    @Transactional
    public BlogPostDetailResponse updatePost(Long postId, Long userId, BlogPostUpdateRequest request) {
        BlogPost post = blogPostRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("BlogPost", "id", postId));

        if (!post.getAuthor().getId().equals(userId)) {
            throw new BusinessException("You can only edit your own posts");
        }

        if (request.title() != null) {
            post.setTitle(request.title());
        }
        if (request.content() != null) {
            post.setContent(request.content());
        }

        post = blogPostRepository.save(post);

        List<CommentResponse> comments = commentRepository
                .findByBlogPostIdAndParentIsNullOrderByCreatedAtAsc(postId)
                .stream()
                .map(CommentResponse::from)
                .toList();

        return BlogPostDetailResponse.from(post, comments);
    }

    @Transactional
    public String vote(Long postId, Long userId, VoteRequest request) {
        BlogPost post = blogPostRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("BlogPost", "id", postId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        BlogVote.VoteType newVoteType = BlogVote.VoteType.valueOf(request.voteType());
        Optional<BlogVote> existingVote = blogVoteRepository.findByBlogPostIdAndUserId(postId, userId);

        if (existingVote.isPresent()) {
            BlogVote vote = existingVote.get();
            if (vote.getVoteType() == newVoteType) {
                // Remove vote (toggle off)
                if (newVoteType == BlogVote.VoteType.UPVOTE) {
                    post.setUpvotes(post.getUpvotes() - 1);
                } else {
                    post.setDownvotes(post.getDownvotes() - 1);
                }
                blogVoteRepository.delete(vote);
                blogPostRepository.save(post);
                return "Vote removed";
            } else {
                // Switch vote
                if (newVoteType == BlogVote.VoteType.UPVOTE) {
                    post.setUpvotes(post.getUpvotes() + 1);
                    post.setDownvotes(post.getDownvotes() - 1);
                } else {
                    post.setUpvotes(post.getUpvotes() - 1);
                    post.setDownvotes(post.getDownvotes() + 1);
                }
                vote.setVoteType(newVoteType);
                blogVoteRepository.save(vote);
                blogPostRepository.save(post);
                return "Vote changed to " + newVoteType;
            }
        } else {
            // New vote
            BlogVote vote = new BlogVote();
            vote.setBlogPost(post);
            vote.setUser(user);
            vote.setVoteType(newVoteType);
            blogVoteRepository.save(vote);

            if (newVoteType == BlogVote.VoteType.UPVOTE) {
                post.setUpvotes(post.getUpvotes() + 1);
            } else {
                post.setDownvotes(post.getDownvotes() + 1);
            }
            blogPostRepository.save(post);
            return "Voted " + newVoteType;
        }
    }

    @Transactional
    public CommentResponse addComment(Long postId, Long userId, CommentCreateRequest request) {
        BlogPost post = blogPostRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("BlogPost", "id", postId));

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Comment comment = new Comment();
        comment.setBlogPost(post);
        comment.setAuthor(author);
        comment.setContent(request.content());

        if (request.parentId() != null) {
            Comment parent = commentRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", request.parentId()));
            if (!parent.getBlogPost().getId().equals(postId)) {
                throw new BusinessException("Parent comment does not belong to this post");
            }
            comment.setParent(parent);
        }

        comment = commentRepository.save(comment);
        return CommentResponse.from(comment);
    }
}
