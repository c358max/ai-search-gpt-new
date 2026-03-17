package com.example.aisearch.controller;

import com.example.aisearch.service.indexing.orchestration.exception.InvalidRestoreTargetException;
import com.example.aisearch.service.indexing.orchestration.exception.RestoreTargetNotFoundException;
import com.example.aisearch.service.search.exception.InvalidSearchRequestException;
import com.example.aisearch.service.synonym.exception.InvalidSynonymReloadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidRestoreTargetException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleInvalidRestoreTarget(InvalidRestoreTargetException e) {
        return new ApiErrorResponse("BAD_REQUEST", e.getMessage());
    }

    @ExceptionHandler(RestoreTargetNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleRestoreTargetNotFound(RestoreTargetNotFoundException e) {
        return new ApiErrorResponse("NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(InvalidSearchRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleInvalidSearchRequest(InvalidSearchRequestException e) {
        return new ApiErrorResponse("INVALID_SEARCH_REQUEST", e.getMessage());
    }

    @ExceptionHandler(InvalidSynonymReloadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleInvalidSynonymReloadRequest(InvalidSynonymReloadRequestException e) {
        return new ApiErrorResponse("INVALID_SYNONYM_RELOAD_REQUEST", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleIllegalArgument(IllegalArgumentException e) {
        return new ApiErrorResponse("BAD_REQUEST", e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleIllegalState(IllegalStateException e) {
        return new ApiErrorResponse("INTERNAL_SERVER_ERROR", e.getMessage());
    }
}
