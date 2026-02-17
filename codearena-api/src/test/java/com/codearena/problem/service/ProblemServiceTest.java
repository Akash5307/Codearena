package com.codearena.problem.service;

import com.codearena.common.exception.ResourceNotFoundException;
import com.codearena.common.service.MinioService;
import com.codearena.problem.dto.ProblemCreateRequest;
import com.codearena.problem.dto.ProblemDetailResponse;
import com.codearena.problem.dto.ProblemUpdateRequest;
import com.codearena.problem.entity.Problem;
import com.codearena.problem.entity.Tag;
import com.codearena.problem.repository.ProblemRepository;
import com.codearena.problem.repository.TagRepository;
import com.codearena.problem.repository.TestCaseRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProblemServiceTest {

    @Mock private ProblemRepository problemRepository;
    @Mock private TagRepository tagRepository;
    @Mock private TestCaseRepository testCaseRepository;
    @Mock private UserRepository userRepository;
    @Mock private MinioService minioService;

    private ProblemService problemService;

    @BeforeEach
    void setUp() {
        problemService = new ProblemService(problemRepository, tagRepository,
                testCaseRepository, userRepository, minioService);
    }

    @Test
    void createProblem_success() {
        User author = new User();
        author.setId(1L);
        author.setUsername("setter");

        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(problemRepository.existsBySlug(anyString())).thenReturn(false);
        when(tagRepository.findByName("dp")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> {
            Tag t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });
        when(problemRepository.save(any(Problem.class))).thenAnswer(inv -> {
            Problem p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProblemCreateRequest request = new ProblemCreateRequest(
                "Two Sum", "Find two numbers...", "Array of integers",
                "Two indices", "EASY", 1000, 256, List.of("dp")
        );

        ProblemDetailResponse response = problemService.createProblem(1L, request);

        assertThat(response.title()).isEqualTo("Two Sum");
        assertThat(response.slug()).isNotBlank();
        verify(problemRepository).save(any(Problem.class));
    }

    @Test
    void createProblem_authorNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ProblemCreateRequest request = new ProblemCreateRequest(
                "Title", "Statement", null, null, null, null, null, null
        );

        assertThatThrownBy(() -> problemService.createProblem(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getProblemBySlug_notFound_throws() {
        when(problemRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> problemService.getProblemBySlug("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getProblemBySlug_success() {
        User author = new User();
        author.setId(1L);
        author.setUsername("setter");

        Problem problem = new Problem();
        problem.setId(1L);
        problem.setTitle("Two Sum");
        problem.setSlug("two-sum");
        problem.setStatement("Find two numbers...");
        problem.setTimeLimitMs(2000);
        problem.setMemoryLimitMb(256);
        problem.setIsPublished(true);
        problem.setAuthor(author);

        when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.of(problem));
        when(testCaseRepository.findByProblemIdAndIsSampleTrueOrderByOrderIndex(1L)).thenReturn(List.of());

        ProblemDetailResponse response = problemService.getProblemBySlug("two-sum");

        assertThat(response.title()).isEqualTo("Two Sum");
        assertThat(response.slug()).isEqualTo("two-sum");
    }

    @Test
    void updateProblem_success() {
        User author = new User();
        author.setId(1L);
        author.setUsername("setter");

        Problem problem = new Problem();
        problem.setId(1L);
        problem.setTitle("Old Title");
        problem.setSlug("old-title");
        problem.setStatement("Old statement");
        problem.setTimeLimitMs(2000);
        problem.setMemoryLimitMb(256);
        problem.setIsPublished(false);
        problem.setAuthor(author);

        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        when(problemRepository.existsBySlug(anyString())).thenReturn(false);
        when(problemRepository.save(any(Problem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(testCaseRepository.findByProblemIdAndIsSampleTrueOrderByOrderIndex(1L)).thenReturn(List.of());

        ProblemUpdateRequest request = new ProblemUpdateRequest(
                "New Title", null, null, null, null, null, null, true, null
        );

        ProblemDetailResponse response = problemService.updateProblem(1L, request);

        assertThat(response.title()).isEqualTo("New Title");
        assertThat(response.isPublished()).isTrue();
    }
}
