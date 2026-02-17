package com.codearena.blog.controller;

import com.codearena.blog.dto.*;
import com.codearena.blog.service.BlogService;
import com.codearena.common.exception.GlobalExceptionHandler;
import com.codearena.common.util.JwtUtil;
import com.codearena.config.JwtAuthenticationFilter;
import com.codearena.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BlogController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class BlogControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private BlogService blogService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private RedisTemplate<String, Object> redisTemplate;

    private UsernamePasswordAuthenticationToken userAuth() {
        return new UsernamePasswordAuthenticationToken(
                1L, "testuser", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void listPosts_returnsOk() throws Exception {
        var response = new BlogPostListResponse(
                1L, "Test Post", "author", 5, 1, 3, LocalDateTime.now());
        when(blogService.listPosts(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/blogs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Test Post"))
                .andExpect(jsonPath("$.data.content[0].upvotes").value(5));
    }

    @Test
    void getPost_returnsOk() throws Exception {
        var response = new BlogPostDetailResponse(
                1L, "Test Post", "Content here", "author",
                5, 1, List.of(), LocalDateTime.now(), LocalDateTime.now());
        when(blogService.getPost(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/blogs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Test Post"))
                .andExpect(jsonPath("$.data.authorUsername").value("author"));
    }

    @Test
    void createPost_withAuth_returnsCreated() throws Exception {
        var request = new BlogPostCreateRequest("My Post", "Post content");
        var response = new BlogPostDetailResponse(
                1L, "My Post", "Post content", "testuser",
                0, 0, List.of(), LocalDateTime.now(), null);
        when(blogService.createPost(eq(1L), any(BlogPostCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/blogs")
                        .with(authentication(userAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("My Post"));
    }

    @Test
    void createPost_withoutAuth_returns403() throws Exception {
        var request = new BlogPostCreateRequest("My Post", "Post content");

        mockMvc.perform(post("/api/v1/blogs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void vote_withAuth_returnsOk() throws Exception {
        var request = new VoteRequest("UPVOTE");
        when(blogService.vote(eq(1L), eq(1L), any(VoteRequest.class)))
                .thenReturn("Voted UPVOTE");

        mockMvc.perform(post("/api/v1/blogs/1/vote")
                        .with(authentication(userAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Voted UPVOTE"));
    }

    @Test
    void addComment_withAuth_returnsCreated() throws Exception {
        var request = new CommentCreateRequest("Great post!", null);
        var response = new CommentResponse(
                1L, "testuser", "Great post!", null, List.of(),
                LocalDateTime.now(), null);
        when(blogService.addComment(eq(1L), eq(1L), any(CommentCreateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/blogs/1/comments")
                        .with(authentication(userAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.content").value("Great post!"));
    }
}
