package com.codearena.contest.service;

import com.codearena.common.exception.BusinessException;
import com.codearena.common.exception.ResourceNotFoundException;
import com.codearena.contest.dto.*;
import com.codearena.contest.entity.Contest;
import com.codearena.contest.entity.ContestProblem;
import com.codearena.contest.entity.ContestRegistration;
import com.codearena.contest.repository.ContestProblemRepository;
import com.codearena.contest.repository.ContestRegistrationRepository;
import com.codearena.contest.repository.ContestRepository;
import com.codearena.problem.entity.Problem;
import com.codearena.problem.repository.ProblemRepository;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class ContestService {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private final ContestRepository contestRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final ContestRegistrationRepository registrationRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;

    public ContestService(ContestRepository contestRepository,
                          ContestProblemRepository contestProblemRepository,
                          ContestRegistrationRepository registrationRepository,
                          ProblemRepository problemRepository,
                          UserRepository userRepository) {
        this.contestRepository = contestRepository;
        this.contestProblemRepository = contestProblemRepository;
        this.registrationRepository = registrationRepository;
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<ContestListResponse> listContests(String status, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Page<Contest> page;

        if ("upcoming".equalsIgnoreCase(status)) {
            page = contestRepository.findUpcoming(now, pageable);
        } else if ("running".equalsIgnoreCase(status)) {
            page = contestRepository.findRunning(now, pageable);
        } else if ("past".equalsIgnoreCase(status)) {
            page = contestRepository.findPast(now, pageable);
        } else {
            page = contestRepository.findAll(pageable);
        }

        return page.map(ContestListResponse::from);
    }

    @Transactional(readOnly = true)
    public ContestDetailResponse getContestBySlug(String slug) {
        Contest contest = contestRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Contest", "slug", slug));

        long regCount = registrationRepository.countByContestId(contest.getId());
        List<ContestProblemResponse> problems = contestProblemRepository
                .findByContestIdOrderByOrderIndex(contest.getId())
                .stream()
                .map(ContestProblemResponse::from)
                .toList();

        return ContestDetailResponse.from(contest, regCount, problems);
    }

    @Transactional
    public ContestDetailResponse createContest(Long authorId, ContestCreateRequest request) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", authorId));

        if (request.startTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Contest start time must be in the future");
        }

        Contest.ContestType type;
        try {
            type = Contest.ContestType.valueOf(request.type().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid contest type: " + request.type());
        }

        Contest contest = new Contest();
        contest.setTitle(request.title());
        contest.setSlug(generateSlug(request.title()));
        contest.setDescription(request.description());
        contest.setType(type);
        contest.setStartTime(request.startTime());
        contest.setDurationMinutes(request.durationMinutes());
        contest.setIsRated(request.isRated() != null ? request.isRated() : true);
        contest.setAuthor(author);

        contest = contestRepository.save(contest);

        if (request.problems() != null) {
            for (ContestProblemRequest cpReq : request.problems()) {
                Problem problem = problemRepository.findById(cpReq.problemId())
                        .orElseThrow(() -> new ResourceNotFoundException("Problem", "id", cpReq.problemId()));

                ContestProblem cp = new ContestProblem();
                cp.setContest(contest);
                cp.setProblem(problem);
                cp.setLabel(cpReq.label());
                cp.setOrderIndex(cpReq.orderIndex());
                cp.setPoints(cpReq.points());
                contestProblemRepository.save(cp);
            }
        }

        long regCount = 0;
        List<ContestProblemResponse> problems = contestProblemRepository
                .findByContestIdOrderByOrderIndex(contest.getId())
                .stream()
                .map(ContestProblemResponse::from)
                .toList();

        return ContestDetailResponse.from(contest, regCount, problems);
    }

    @Transactional
    public String registerForContest(Long contestId, Long userId) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new ResourceNotFoundException("Contest", "id", contestId));

        if (contest.getState() == Contest.ContestState.ENDED) {
            throw new BusinessException("Contest has already ended");
        }

        if (registrationRepository.existsByContestIdAndUserId(contestId, userId)) {
            throw new BusinessException("Already registered for this contest");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        ContestRegistration reg = new ContestRegistration();
        reg.setContest(contest);
        reg.setUser(user);
        registrationRepository.save(reg);

        return "Successfully registered for contest: " + contest.getTitle();
    }

    private String generateSlug(String title) {
        String noWhitespace = WHITESPACE.matcher(title).replaceAll("-");
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("").toLowerCase(Locale.ENGLISH);

        String baseSlug = slug;
        int counter = 1;
        while (contestRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }
        return slug;
    }
}
