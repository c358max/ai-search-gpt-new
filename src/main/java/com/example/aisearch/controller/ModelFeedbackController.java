package com.example.aisearch.controller;

import com.example.aisearch.controller.dto.ModelFeedbackResponseDto;
import com.example.aisearch.controller.dto.ModelFeedbackSaveRequestDto;
import com.example.aisearch.controller.dto.ModelFeedbackSummaryResponseDto;
import com.example.aisearch.service.feedback.ModelFeedbackService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@CrossOrigin(origins = "*")
public class ModelFeedbackController {

    private final ModelFeedbackService modelFeedbackService;

    public ModelFeedbackController(ModelFeedbackService modelFeedbackService) {
        this.modelFeedbackService = modelFeedbackService;
    }

    @PostMapping("/api/model-feedback")
    public ModelFeedbackResponseDto save(@Valid @RequestBody ModelFeedbackSaveRequestDto request) {
        return ModelFeedbackResponseDto.from(modelFeedbackService.save(request.query(), request.score()));
    }

    @GetMapping("/api/model-feedback")
    public ModelFeedbackResponseDto getByQuery(@RequestParam("q") String query) {
        return ModelFeedbackResponseDto.from(modelFeedbackService.getByQuery(query));
    }

    @GetMapping("/api/model-feedback/summary")
    public ModelFeedbackSummaryResponseDto getSummary() {
        return ModelFeedbackSummaryResponseDto.from(modelFeedbackService.getOverall());
    }
}
