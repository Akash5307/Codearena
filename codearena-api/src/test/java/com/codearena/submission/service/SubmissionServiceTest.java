package com.codearena.submission.service;

import com.codearena.common.exception.BusinessException;
import com.codearena.common.exception.ResourceNotFoundException;
import com.codearena.contest.entity.Contest;
import com.codearena.contest.repository.ContestRegistrationRepository;
import com.codearena.contest.repository.ContestRepository;
import com.codearena.problem.entity.Problem;
import com.codearena.problem.repository.ProblemRepository;
import com.codearena.submission.dto.SubmissionDetailResponse;
import com.codearena.submission.dto.SubmitRequest;
import com.codearena.submission.entity.Submission;
import com.codearena.submission.repository.SubmissionRepository;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock private SubmissionRepository submissionRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProblemRepository problemRepository;
    @Mock private ContestRepository contestRepository;
    @Mock private ContestRegistrationRepository registrationRepository;
    @Mock private SubmissionPublisher submissionPublisher;

    private SubmissionService submissionService;

    private User testUser;
    private Problem testProblem;

    @BeforeEach
    void setUp() {
        submissionService = new SubmissionService(submissionRepository, userRepository,
                problemRepository, contestRepository, registrationRepository, submissionPublisher);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testProblem = new Problem();
        testProblem.setId(1L);
        testProblem.setTitle("Two Sum");
        testProblem.setIsPublished(true);
        testProblem.setTimeLimitMs(2000);
        testProblem.setMemoryLimitMb(256);
    }

    @Test
    void submit_success() {
        when(submissionRepository.existsRecentByUserId(eq(1L), any())).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(problemRepository.findById(1L)).thenReturn(Optional.of(testProblem));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        SubmitRequest request = new SubmitRequest(1L, null, "CPP", "int main() {}");
        SubmissionDetailResponse response = submissionService.submit(1L, request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.language()).isEqualTo("CPP");
        assertThat(response.verdict()).isEqualTo("PENDING");
        verify(submissionPublisher).publishJudgeTask(any());
    }

    @Test
    void submit_rateLimited_throws() {
        when(submissionRepository.existsRecentByUserId(eq(1L), any())).thenReturn(true);

        SubmitRequest request = new SubmitRequest(1L, null, "CPP", "int main() {}");

        assertThatThrownBy(() -> submissionService.submit(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("wait");
    }

    @Test
    void submit_unpublishedProblem_throws() {
        testProblem.setIsPublished(false);
        when(submissionRepository.existsRecentByUserId(eq(1L), any())).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(problemRepository.findById(1L)).thenReturn(Optional.of(testProblem));

        SubmitRequest request = new SubmitRequest(1L, null, "CPP", "int main() {}");

        assertThatThrownBy(() -> submissionService.submit(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Problem is not published");
    }

    @Test
    void submit_invalidLanguage_throws() {
        when(submissionRepository.existsRecentByUserId(eq(1L), any())).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(problemRepository.findById(1L)).thenReturn(Optional.of(testProblem));

        SubmitRequest request = new SubmitRequest(1L, null, "COBOL", "code");

        assertThatThrownBy(() -> submissionService.submit(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Unsupported language");
    }

    @Test
    void submit_contestNotRunning_throws() {
        Contest contest = new Contest();
        contest.setId(1L);
        contest.setStartTime(LocalDateTime.now().plusHours(1));
        contest.setDurationMinutes(120);

        when(submissionRepository.existsRecentByUserId(eq(1L), any())).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(problemRepository.findById(1L)).thenReturn(Optional.of(testProblem));
        when(contestRepository.findById(1L)).thenReturn(Optional.of(contest));

        SubmitRequest request = new SubmitRequest(1L, 1L, "CPP", "int main() {}");

        assertThatThrownBy(() -> submissionService.submit(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Contest is not currently running");
    }

    @Test
    void submit_notRegisteredForContest_throws() {
        Contest contest = new Contest();
        contest.setId(1L);
        contest.setStartTime(LocalDateTime.now().minusMinutes(10));
        contest.setDurationMinutes(120);

        when(submissionRepository.existsRecentByUserId(eq(1L), any())).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(problemRepository.findById(1L)).thenReturn(Optional.of(testProblem));
        when(contestRepository.findById(1L)).thenReturn(Optional.of(contest));
        when(registrationRepository.existsByContestIdAndUserId(1L, 1L)).thenReturn(false);

        SubmitRequest request = new SubmitRequest(1L, 1L, "CPP", "int main() {}");

        assertThatThrownBy(() -> submissionService.submit(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("You are not registered for this contest");
    }

    @Test
    void getSubmission_notFound_throws() {
        when(submissionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.getSubmission(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
