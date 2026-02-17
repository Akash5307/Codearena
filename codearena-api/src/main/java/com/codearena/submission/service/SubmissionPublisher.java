package com.codearena.submission.service;

import com.codearena.config.RabbitMQConfig;
import com.codearena.submission.dto.JudgeTask;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class SubmissionPublisher {

    private final RabbitTemplate rabbitTemplate;

    public SubmissionPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishJudgeTask(JudgeTask task) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.JUDGE_QUEUE, task);
    }
}
