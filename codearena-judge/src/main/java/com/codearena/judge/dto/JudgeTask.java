package com.codearena.judge.dto;

import java.io.Serializable;

public record JudgeTask(
        Long submissionId,
        Long problemId,
        Long contestId,
        String language,
        String sourceCode,
        int timeLimitMs,
        int memoryLimitMb
) implements Serializable {}
