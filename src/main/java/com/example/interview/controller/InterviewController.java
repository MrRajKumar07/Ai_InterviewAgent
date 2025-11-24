package com.example.interview.controller;

import com.example.interview.model.Answer;
import com.example.interview.model.InterviewConfig;
import com.example.interview.model.InterviewFeedback;
import com.example.interview.service.InterviewService;
import com.example.interview.service.InterviewService.NextQuestionResponse;
import com.example.interview.service.InterviewService.StartInterviewResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interview")
@CrossOrigin
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @GetMapping
    public String health() {
        return "Interview API is up ✅";
    }

    @PostMapping("/start")
    public StartInterviewResponse start(@RequestBody InterviewConfig config) {
        return interviewService.startInterview(config);
    }

    @PostMapping("/{sessionId}/answer")
    public NextQuestionResponse answer(
            @PathVariable("sessionId") String sessionId,
            @RequestBody Answer answer
    ) {
        return interviewService.submitAnswer(sessionId, answer);
    }

    @PostMapping("/{sessionId}/finish")
    public InterviewFeedback finish(@PathVariable("sessionId") String sessionId) {
        return interviewService.finishInterview(sessionId);
    }
}
