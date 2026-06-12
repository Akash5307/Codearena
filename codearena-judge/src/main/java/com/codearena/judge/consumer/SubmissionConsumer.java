package com.codearena.judge.consumer;

import com.codearena.judge.config.RabbitMQConfig;
import com.codearena.judge.dto.JudgeResult;
import com.codearena.judge.dto.JudgeTask;
import com.codearena.judge.service.JudgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class SubmissionConsumer {

    private static final Logger log = LoggerFactory.getLogger(SubmissionConsumer.class);

    private final JudgeService judgeService;
    private final RabbitTemplate rabbitTemplate;

    public SubmissionConsumer(JudgeService judgeService, RabbitTemplate rabbitTemplate) {
        this.judgeService = judgeService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.JUDGE_QUEUE)
    public void onSubmission(JudgeTask task) {
        log.info("Received judge task for submission {}, language={}", task.submissionId(), task.language());

        // Surface progress to pollers: PENDING -> JUDGING as soon as a worker picks
        // the task up. The API listener applies this only while still PENDING.
        rabbitTemplate.convertAndSend(RabbitMQConfig.RESULT_QUEUE,
                new JudgeResult(task.submissionId(), "JUDGING", null, null, 0, 0));

        try {
            JudgeResult result = judgeService.judge(task);
            log.info("Submission {} verdict: {}, passed {}/{}", task.submissionId(),
                    result.verdict(), result.testCasesPassed(), result.totalTestCases());

            rabbitTemplate.convertAndSend(RabbitMQConfig.RESULT_QUEUE, result);
        } catch (Exception e) {
            log.error("Failed to judge submission {}", task.submissionId(), e);

            JudgeResult errorResult = new JudgeResult(
                    task.submissionId(), "RE", null, null, 0, 0);
            rabbitTemplate.convertAndSend(RabbitMQConfig.RESULT_QUEUE, errorResult);
        }
    }
}
