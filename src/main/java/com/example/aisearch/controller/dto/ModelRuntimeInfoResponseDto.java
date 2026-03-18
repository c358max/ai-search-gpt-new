package com.example.aisearch.controller.dto;

public record ModelRuntimeInfoResponseDto(
        int dimensions,
        long heapUsedBytes,
        long heapMaxBytes,
        String modelSource
) {
}
