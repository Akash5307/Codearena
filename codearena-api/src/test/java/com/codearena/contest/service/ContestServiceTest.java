package com.codearena.contest.service;

import com.codearena.common.exception.BusinessException;
import com.codearena.common.exception.ResourceNotFoundException;
import com.codearena.contest.dto.ContestCreateRequest;
import com.codearena.contest.dto.ContestDetailResponse;
import com.codearena.contest.entity.Contest;
import com.codearena.contest.entity.ContestRegistration;
import com.codearena.contest.repository.ContestProblemRepository;
import com.codearena.contest.repository.ContestRegistrationRepository;
import com.codearena.contest.repository.ContestRepository;
import com.codearena.problem.repository.ProblemRepository;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContestServiceTest {

    @Mock private ContestRepository contestRepository;
    @Mock private ContestProblemRepository contestProblemRepository;
    @Mock private ContestRegistrationRepository registrationRepository;
    @Mock private ProblemRepository problemRepository;
    @Mock private UserRepository userRepository;

    private ContestService contestService;

    @BeforeEach
    void setUp() {
        contestService = new ContestService(contestRepository, contestProblemRepository,
                registrationRepository, problemRepository, userRepository);
    }

    @Test
    void createContest_success() {
        User author = new User();
        author.setId(1L);
        author.setUsername("admin");

        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(contestRepository.existsBySlug(anyString())).thenReturn(false);
        when(contestRepository.save(any(Contest.class))).thenAnswer(inv -> {
            Contest c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(contestProblemRepository.findByContestIdOrderByOrderIndex(1L)).thenReturn(List.of());

        ContestCreateRequest request = new ContestCreateRequest(
                "Codeforces Round #900", "A fun contest", "ICPC",
                LocalDateTime.now().plusDays(1), 120, true, null
        );

        ContestDetailResponse response = contestService.createContest(1L, request);

        assertThat(response.title()).isEqualTo("Codeforces Round #900");
        assertThat(response.type()).isEqualTo("ICPC");
        assertThat(response.state()).isEqualTo("BEFORE");
        verify(contestRepository).save(any(Contest.class));
    }

    @Test
    void createContest_pastStartTime_throws() {
        User author = new User();
        author.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));

        ContestCreateRequest request = new ContestCreateRequest(
                "Old Contest", null, "ICPC",
                LocalDateTime.now().minusDays(1), 120, true, null
        );

        assertThatThrownBy(() -> contestService.createContest(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Contest start time must be in the future");
    }

    @Test
    void createContest_invalidType_throws() {
        User author = new User();
        author.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));

        ContestCreateRequest request = new ContestCreateRequest(
                "Contest", null, "INVALID",
                LocalDateTime.now().plusDays(1), 120, true, null
        );

        assertThatThrownBy(() -> contestService.createContest(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid contest type");
    }

    @Test
    void registerForContest_success() {
        Contest contest = new Contest();
        contest.setId(1L);
        contest.setTitle("Test Contest");
        contest.setStartTime(LocalDateTime.now().plusHours(1));
        contest.setDurationMinutes(120);

        User user = new User();
        user.setId(2L);

        when(contestRepository.findById(1L)).thenReturn(Optional.of(contest));
        when(registrationRepository.existsByContestIdAndUserId(1L, 2L)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(registrationRepository.save(any())).thenReturn(new ContestRegistration());

        String result = contestService.registerForContest(1L, 2L);

        assertThat(result).contains("Successfully registered");
        verify(registrationRepository).save(any(ContestRegistration.class));
    }

    @Test
    void registerForContest_alreadyRegistered_throws() {
        Contest contest = new Contest();
        contest.setId(1L);
        contest.setStartTime(LocalDateTime.now().plusHours(1));
        contest.setDurationMinutes(120);

        when(contestRepository.findById(1L)).thenReturn(Optional.of(contest));
        when(registrationRepository.existsByContestIdAndUserId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> contestService.registerForContest(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Already registered for this contest");
    }

    @Test
    void registerForContest_ended_throws() {
        Contest contest = new Contest();
        contest.setId(1L);
        contest.setStartTime(LocalDateTime.now().minusHours(5));
        contest.setDurationMinutes(120);

        when(contestRepository.findById(1L)).thenReturn(Optional.of(contest));

        assertThatThrownBy(() -> contestService.registerForContest(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Contest has already ended");
    }

    @Test
    void getContestBySlug_notFound_throws() {
        when(contestRepository.findBySlug("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contestService.getContestBySlug("nope"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
