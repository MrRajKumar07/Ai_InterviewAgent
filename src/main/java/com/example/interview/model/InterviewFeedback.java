package com.example.interview.model;

import java.util.List;
import java.util.Map;

public class InterviewFeedback {

    private String overallSummary;
    private Map<String, Integer> scores;
    private List<String> strengths;
    private List<String> areasToImprove;
    private List<SampleImprovedAnswer> sampleImprovedAnswers;

    public InterviewFeedback() {
    }

    public String getOverallSummary() {
        return overallSummary;
    }

    public void setOverallSummary(String overallSummary) {
        this.overallSummary = overallSummary;
    }

    public Map<String, Integer> getScores() {
        return scores;
    }

    public void setScores(Map<String, Integer> scores) {
        this.scores = scores;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths;
    }

    public List<String> getAreasToImprove() {
        return areasToImprove;
    }

    public void setAreasToImprove(List<String> areasToImprove) {
        this.areasToImprove = areasToImprove;
    }

    public List<SampleImprovedAnswer> getSampleImprovedAnswers() {
        return sampleImprovedAnswers;
    }

    public void setSampleImprovedAnswers(List<SampleImprovedAnswer> sampleImprovedAnswers) {
        this.sampleImprovedAnswers = sampleImprovedAnswers;
    }

    public static class SampleImprovedAnswer {
        private String question;
        private String improvedAnswer;

        public SampleImprovedAnswer() {
        }

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public String getImprovedAnswer() {
            return improvedAnswer;
        }

        public void setImprovedAnswer(String improvedAnswer) {
            this.improvedAnswer = improvedAnswer;
        }
    }
}
