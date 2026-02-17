package com.codearena.submission.dto;

import java.io.Serializable;

public record JudgeResult(
        Long submissionId,
        String verdict,
        Integer timeUsedMs,
        Integer memoryUsedKb,
        int testCasesPassed,
        int totalTestCases
) implements Serializable {}
