package com.codearena.problem.dto;

import com.codearena.problem.entity.TestCase;

public record TestCaseResponse(
        Long id,
        String inputUrl,
        String expectedOutputUrl,
        boolean isSample,
        int orderIndex
) {
    public static TestCaseResponse from(TestCase tc) {
        return new TestCaseResponse(
                tc.getId(),
                tc.getInputUrl(),
                tc.getExpectedOutputUrl(),
                tc.getIsSample(),
                tc.getOrderIndex()
        );
    }
}
