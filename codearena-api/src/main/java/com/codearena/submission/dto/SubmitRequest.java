package com.codearena.submission.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Code submission request")
public record SubmitRequest(
        @NotNull(message = "Problem ID is required")
        @Schema(description = "Problem ID to submit against", example = "1")
        Long problemId,

        @Schema(description = "Contest ID (optional, for contest submissions)", example = "1")
        Long contestId,

        @NotBlank(message = "Language is required")
        @Schema(description = "Programming language", example = "CPP",
                allowableValues = {"JAVA", "CPP", "C", "PYTHON", "JAVASCRIPT", "GO", "RUST", "KOTLIN"})
        String language,

        @NotBlank(message = "Source code is required")
        @Schema(description = "Source code to judge", example = "#include<bits/stdc++.h>\nusing namespace std;\nint main(){}")
        String sourceCode
) {}
