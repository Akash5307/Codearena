package com.codearena.problem.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Sample test case with its actual input/output text")
public record SampleTestCaseResponse(
        @Schema(description = "Test case id") Long id,
        @Schema(description = "Display order") int orderIndex,
        @Schema(description = "Sample input text") String input,
        @Schema(description = "Expected output text") String output
) {}
