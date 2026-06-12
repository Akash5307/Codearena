package com.codearena.problem.service;

import com.codearena.common.exception.BusinessException;
import com.codearena.common.exception.ResourceNotFoundException;
import com.codearena.common.service.MinioService;
import com.codearena.problem.dto.*;
import com.codearena.problem.entity.Problem;
import com.codearena.problem.entity.Tag;
import com.codearena.problem.entity.TestCase;
import com.codearena.problem.repository.ProblemRepository;
import com.codearena.problem.repository.TagRepository;
import com.codearena.problem.repository.TestCaseRepository;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ProblemService {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private final ProblemRepository problemRepository;
    private final TagRepository tagRepository;
    private final TestCaseRepository testCaseRepository;
    private final UserRepository userRepository;
    private final MinioService minioService;

    public ProblemService(ProblemRepository problemRepository,
                          TagRepository tagRepository,
                          TestCaseRepository testCaseRepository,
                          UserRepository userRepository,
                          MinioService minioService) {
        this.problemRepository = problemRepository;
        this.tagRepository = tagRepository;
        this.testCaseRepository = testCaseRepository;
        this.userRepository = userRepository;
        this.minioService = minioService;
    }

    @Transactional(readOnly = true)
    public Page<ProblemListResponse> listProblems(String title, String difficulty, String tagName, Pageable pageable) {
        Problem.Difficulty diff = null;
        if (difficulty != null && !difficulty.isBlank()) {
            try {
                diff = Problem.Difficulty.valueOf(difficulty.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid difficulty: " + difficulty);
            }
        }
        return problemRepository.findPublishedWithFilters(
                title, diff, tagName, pageable
        ).map(ProblemListResponse::from);
    }

    // readOnly tx keeps the session open while DTO mappers walk lazy associations (author, tags)
    @Transactional(readOnly = true)
    @Cacheable(value = "problemDetail", key = "#slug")
    public ProblemDetailResponse getProblemBySlug(String slug) {
        Problem problem = problemRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Problem", "slug", slug));
        List<TestCaseResponse> sampleCases = testCaseRepository
                .findByProblemIdAndIsSampleTrueOrderByOrderIndex(problem.getId())
                .stream()
                .map(TestCaseResponse::from)
                .toList();
        return ProblemDetailResponse.from(problem, sampleCases);
    }

    // Inlines the actual input/output text of a problem's SAMPLE test cases (only).
    // Hidden test cases are never exposed here — this is what makes sample I/O
    // displayable on the problem page without leaking the judging data.
    @Transactional(readOnly = true)
    public List<SampleTestCaseResponse> getSampleTestCases(String slug) {
        Problem problem = problemRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Problem", "slug", slug));
        return testCaseRepository
                .findByProblemIdAndIsSampleTrueOrderByOrderIndex(problem.getId())
                .stream()
                .map(tc -> new SampleTestCaseResponse(
                        tc.getId(),
                        tc.getOrderIndex(),
                        minioService.downloadAsString(tc.getInputUrl()),
                        minioService.downloadAsString(tc.getExpectedOutputUrl())))
                .toList();
    }

    @Transactional
    @CacheEvict(value = "tags", allEntries = true)
    public ProblemDetailResponse createProblem(Long authorId, ProblemCreateRequest request) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", authorId));

        Problem problem = new Problem();
        problem.setTitle(request.title());
        problem.setSlug(generateSlug(request.title()));
        problem.setStatement(request.statement());
        problem.setInputFormat(request.inputFormat());
        problem.setOutputFormat(request.outputFormat());
        problem.setAuthor(author);

        if (request.difficulty() != null) {
            problem.setDifficulty(Problem.Difficulty.valueOf(request.difficulty().toUpperCase()));
        }
        if (request.timeLimitMs() != null) {
            problem.setTimeLimitMs(request.timeLimitMs());
        }
        if (request.memoryLimitMb() != null) {
            problem.setMemoryLimitMb(request.memoryLimitMb());
        }

        if (request.tags() != null) {
            problem.setTags(resolveOrCreateTags(request.tags()));
        }

        problem = problemRepository.save(problem);
        return ProblemDetailResponse.from(problem, List.of());
    }

    @Transactional
    @CacheEvict(value = {"problemDetail", "tags"}, allEntries = true)
    public ProblemDetailResponse updateProblem(Long problemId, ProblemUpdateRequest request) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResourceNotFoundException("Problem", "id", problemId));

        if (request.title() != null) {
            problem.setTitle(request.title());
            problem.setSlug(generateSlug(request.title()));
        }
        if (request.statement() != null) {
            problem.setStatement(request.statement());
        }
        if (request.inputFormat() != null) {
            problem.setInputFormat(request.inputFormat());
        }
        if (request.outputFormat() != null) {
            problem.setOutputFormat(request.outputFormat());
        }
        if (request.difficulty() != null) {
            problem.setDifficulty(Problem.Difficulty.valueOf(request.difficulty().toUpperCase()));
        }
        if (request.timeLimitMs() != null) {
            problem.setTimeLimitMs(request.timeLimitMs());
        }
        if (request.memoryLimitMb() != null) {
            problem.setMemoryLimitMb(request.memoryLimitMb());
        }
        if (request.isPublished() != null) {
            problem.setIsPublished(request.isPublished());
        }
        if (request.tags() != null) {
            problem.setTags(resolveOrCreateTags(request.tags()));
        }

        problem = problemRepository.save(problem);
        List<TestCaseResponse> sampleCases = testCaseRepository
                .findByProblemIdAndIsSampleTrueOrderByOrderIndex(problem.getId())
                .stream()
                .map(TestCaseResponse::from)
                .toList();
        return ProblemDetailResponse.from(problem, sampleCases);
    }

    @Transactional
    public TestCaseResponse addTestCase(Long problemId, MultipartFile inputFile,
                                         MultipartFile outputFile, boolean isSample) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResourceNotFoundException("Problem", "id", problemId));

        int nextIndex = testCaseRepository.findByProblemIdOrderByOrderIndex(problemId).size();

        // Deterministic, sort-stable object keys. The judge worker discovers test cases
        // by listing this prefix and pairing sorted keys (input before output), so names
        // must sort by test-case order — random UUIDs here would scramble the pairs.
        String base = "testcases/" + problemId + "/" + String.format("%05d", nextIndex);
        String inputUrl = minioService.uploadFileAs(base + "/input.txt", inputFile);
        String outputUrl = minioService.uploadFileAs(base + "/output.txt", outputFile);

        TestCase testCase = new TestCase();
        testCase.setProblem(problem);
        testCase.setInputUrl(inputUrl);
        testCase.setExpectedOutputUrl(outputUrl);
        testCase.setIsSample(isSample);
        testCase.setOrderIndex(nextIndex);
        testCase = testCaseRepository.save(testCase);

        return TestCaseResponse.from(testCase);
    }

    @Cacheable(value = "tags", key = "'all'")
    public List<TagResponse> getAllTags() {
        return tagRepository.findAll().stream()
                .map(TagResponse::from)
                .toList();
    }

    private Set<Tag> resolveOrCreateTags(List<String> tagNames) {
        Set<Tag> tags = new HashSet<>();
        for (String name : tagNames) {
            String normalized = name.trim().toLowerCase();
            Tag tag = tagRepository.findByName(normalized)
                    .orElseGet(() -> tagRepository.save(new Tag(normalized)));
            tags.add(tag);
        }
        return tags;
    }

    private String generateSlug(String title) {
        String noWhitespace = WHITESPACE.matcher(title).replaceAll("-");
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("").toLowerCase(Locale.ENGLISH);

        // Ensure uniqueness
        String baseSlug = slug;
        int counter = 1;
        while (problemRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }
        return slug;
    }
}
