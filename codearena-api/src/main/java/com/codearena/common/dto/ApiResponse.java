package com.codearena.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response wrapper")
public record ApiResponse<T>(
        @Schema(description = "Whether the request succeeded", example = "true")
        boolean success,

        @Schema(description = "Response payload (null on error)")
        T data,

        @Schema(description = "Error code (null on success)", example = "VALIDATION_ERROR")
        String errorCode,

        @Schema(description = "Error message (null on success)", example = "Username is required")
        String error
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> error(String errorCode, String error) {
        return new ApiResponse<>(false, null, errorCode, error);
    }
}
