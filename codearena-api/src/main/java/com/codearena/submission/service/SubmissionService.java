package com.codearena.submission.service;

import com.codearena.common.exception.BusinessException;
import com.codearena.common.exception.ResourceNotFoundException;
import com.codearena.contest.entity.Contest;
import com.codearena.contest.repository.ContestRegistrationRepository;
import com.codearena.contest.repository.ContestRepository;
import com.codearena.problem.entity.Problem;
import com.codearena.problem.repository.ProblemRepository;
import com.codearena.submission.dto.*;
import com.codearena.submission.entity.Submission;
import com.codearena.submission.repository.SubmissionRepository;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SubmissionService {

    private static final int RATE_LIMIT_SECONDS = 10;

    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final ContestRepository contestRepository;
    private final ContestRegistrationRepository registrationRepository;
    private final SubmissionPublisher submissionPublisher;

    public SubmissionService(SubmissionRepository submissionRepository,
                             UserRepository userRepository,
                             ProblemRepository problemRepository,
                             ContestRepository contestRepository,
                             ContestRegistrationRepository registrationRepository,
                             SubmissionPublisher submissionPublisher) {
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.problemRepository = problemRepository;
        this.contestRepository = contestRepository;
        this.registrationRepository = registrationRepository;
        this.submissionPublisher = submissionPublisher;
    }

    @Transactional
    public SubmissionDetailResponse submit(Long userId, SubmitRequest request) {
        // Rate limiting
        LocalDateTime since = LocalDateTime.now().minusSeconds(RATE_LIMIT_SECONDS);
        if (submissionRepository.existsRecentByUserId(userId, since)) {
            throw new BusinessException("Please wait " + RATE_LIMIT_SECONDS +
                    " seconds between submissions");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Problem problem = problemRepository.findById(request.problemId())
                .orElseThrow(() -> new ResourceNotFoundException("Problem", "id", request.problemId()));

        if (!problem.getIsPublished()) {
            throw new BusinessException("Problem is not published");
        }

        Submission.Language language;
        try {
            language = Submission.Language.valueOf(request.language().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Unsupported language: " + request.language());
        }

        Contest contest = null;
        if (request.contestId() != null) {
            contest = contestRepository.findById(request.contestId())
                    .orElseThrow(() -> new ResourceNotFoundException("Contest", "id", request.contestId()));

            if (contest.getState() != Contest.ContestState.RUNNING) {
                throw new BusinessException("Contest is not currently running");
            }

            if (!registrationRepository.existsByContestIdAndUserId(contest.getId(), userId)) {
                throw new BusinessException("You are not registered for this contest");
            }
        }

        Submission submission = new Submission();
        submission.setUser(user);
        submission.setProblem(problem);
        submission.setContest(contest);
        submission.setLanguage(language);
        submission.setSourceCode(request.sourceCode());
        submission = submissionRepository.save(submission);

        // Publish to judge queue
        JudgeTask task = new JudgeTask(
                submission.getId(),
                problem.getId(),
                contest != null ? contest.getId() : null,
                language.name(),
                request.sourceCode(),
                problem.getTimeLimitMs(),
                problem.getMemoryLimitMb()
        );
        submissionPublisher.publishJudgeTask(task);

        return SubmissionDetailResponse.from(submission);
    }

    // readOnly transactions keep the Hibernate session open while the DTO mappers
    // walk lazy associations (submission.user / submission.problem).
    @Transactional(readOnly = true)
    public SubmissionDetailResponse getSubmission(Long id) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", "id", id));
        return SubmissionDetailResponse.from(submission);
    }

    @Transactional(readOnly = true)
    public Page<SubmissionListResponse> listSubmissions(Long userId, Long problemId,
                                                         String language, String verdict,
                                                         Pageable pageable) {
        Submission.Language lang = null;
        if (language != null && !language.isBlank()) {
            try {
                lang = Submission.Language.valueOf(language.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid language: " + language);
            }
        }

        Submission.Verdict verd = null;
        if (verdict != null && !verdict.isBlank()) {
            try {
                verd = Submission.Verdict.valueOf(verdict.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid verdict: " + verdict);
            }
        }

        return submissionRepository.findWithFilters(userId, problemId, lang, verd, pageable)
                .map(SubmissionListResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<SubmissionListResponse> getUserSubmissions(String username, Pageable pageable) {
        return submissionRepository.findByUserUsernameOrderBySubmittedAtDesc(username, pageable)
                .map(SubmissionListResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<SubmissionListResponse> getMyContestSubmissions(Long contestId, Long userId, Pageable pageable) {
        return submissionRepository.findByContestIdAndUserIdOrderBySubmittedAtDesc(contestId, userId, pageable)
                .map(SubmissionListResponse::from);
    }
}
