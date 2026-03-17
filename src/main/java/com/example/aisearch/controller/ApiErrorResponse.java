package com.example.aisearch.controller;

public record ApiErrorResponse(
        String code,
        String message
) {
}
