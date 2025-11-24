package com.example.interview.model;

public class InterviewConfig {

    private String role;
    private String experienceLevel;
    private String interviewType; // TECHNICAL / BEHAVIORAL / HR / MIXED

    public InterviewConfig() {
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getExperienceLevel() {
        return experienceLevel;
    }

    public void setExperienceLevel(String experienceLevel) {
        this.experienceLevel = experienceLevel;
    }

    public String getInterviewType() {
        return interviewType;
    }

    public void setInterviewType(String interviewType) {
        this.interviewType = interviewType;
    }
}
