package com.codearena.submission.service;

import com.codearena.config.RabbitMQConfig;
import com.codearena.contest.service.StandingsService;
import com.codearena.submission.dto.JudgeResult;
import com.codearena.submission.entity.Submission;
import com.codearena.submission.repository.SubmissionRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class JudgeResultListener {

    private final SubmissionRepository submissionRepository;
    private final StandingsService standingsService;

    public JudgeResultListener(SubmissionRepository submissionRepository,
                                StandingsService standingsService) {
        this.submissionRepository = submissionRepository;
        this.standingsService = standingsService;
    }

    @RabbitListener(queues = RabbitMQConfig.RESULT_QUEUE)
    @Transactional
    public void onJudgeResult(JudgeResult result) {
        submissionRepository.findById(result.submissionId()).ifPresent(submission -> {
            submission.setVerdict(Submission.Verdict.valueOf(result.verdict()));
            submission.setTimeUsedMs(result.timeUsedMs());
            submission.setMemoryUsedKb(result.memoryUsedKb());
            submission.setTestCasesPassed(result.testCasesPassed());
            submission.setTotalTestCases(result.totalTestCases());
            submission.setJudgedAt(LocalDateTime.now());
            submissionRepository.save(submission);

            // Invalidate standings cache if AC during an active contest
            if ("AC".equals(result.verdict()) && submission.getContest() != null) {
                standingsService.invalidateStandingsCache(submission.getContest().getId());
            }
        });
    }
}
