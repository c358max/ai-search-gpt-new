package com.example.aisearch.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModelFeedbackSaveRequestDto(
        @NotBlank String query,
        @Min(1) @Max(5) int score,
        @Size(max = 50) String sortOption,
        @Min(0) Long searchDurationMillis
) {
}
