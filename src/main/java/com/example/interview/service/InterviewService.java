package com.example.interview.service;

import com.example.interview.llm.LlmClient;
import com.example.interview.llm.dto.ChatMessage;
import com.example.interview.model.Answer;
import com.example.interview.model.InterviewConfig;
import com.example.interview.model.InterviewFeedback;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InterviewService {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final Map<String, InterviewSession> sessions = new ConcurrentHashMap<>();

    public InterviewService(LlmClient llmClient) {
        this.llmClient = llmClient;
        this.objectMapper = new ObjectMapper();
    }

    // ---------- DTOs for controller ----------

    public static class StartInterviewResponse {
        private String sessionId;
        private String firstQuestion;

        public StartInterviewResponse() {
        }

        public StartInterviewResponse(String sessionId, String firstQuestion) {
            this.sessionId = sessionId;
            this.firstQuestion = firstQuestion;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getFirstQuestion() {
            return firstQuestion;
        }

        public void setFirstQuestion(String firstQuestion) {
            this.firstQuestion = firstQuestion;
        }
    }

    public static class NextQuestionResponse {
        private String nextQuestion;

        public NextQuestionResponse() {
        }

        public NextQuestionResponse(String nextQuestion) {
            this.nextQuestion = nextQuestion;
        }

        public String getNextQuestion() {
            return nextQuestion;
        }

        public void setNextQuestion(String nextQuestion) {
            this.nextQuestion = nextQuestion;
        }
    }

    // ---------- Core methods ----------

    public StartInterviewResponse startInterview(InterviewConfig config) {
        String sessionId = UUID.randomUUID().toString();
        InterviewSession session = new InterviewSession();
        session.setId(sessionId);
        session.setRole(config.getRole());
        session.setExperienceLevel(config.getExperienceLevel());
        session.setInterviewType(config.getInterviewType());
        sessions.put(sessionId, session);

        String prompt = String.format("""
                You are an AI interviewer for the role: %s.
                Candidate experience level: %s.
                Interview type: %s (technical, behavioral, HR, or mixed).
                Ask the FIRST interview question. Keep it clear and conversational, 1–2 sentences.
                """,
                config.getRole(),
                config.getExperienceLevel(),
                config.getInterviewType()
        );

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", "You are a professional job interviewer."));
        messages.add(new ChatMessage("user", prompt));

        String question = llmClient.chat(messages);

        session.getQuestions().add(question);
        return new StartInterviewResponse(sessionId, question);
    }

    public NextQuestionResponse submitAnswer(String sessionId, Answer answer) {
        InterviewSession session = sessions.get(sessionId);
        if (session == null || session.isFinished()) {
            return new NextQuestionResponse("Session not found or already finished.");
        }

        String lastQuestion = session.getQuestions().isEmpty()
                ? "No previous question."
                : session.getQuestions().get(session.getQuestions().size() - 1);

        session.getAnswers().add(answer.getText());

        String prompt = String.format("""
                You are continuing a mock interview.
                Role: %s, experience: %s, type: %s.
                The last question was: "%s"
                The candidate answered: "%s"

                Based on this answer, ask ONE good follow-up interview question.
                It can be a deeper technical probe, a behavioral follow-up, or a clarification.
                Keep it short (1–2 sentences).
                """,
                session.getRole(),
                session.getExperienceLevel(),
                session.getInterviewType(),
                lastQuestion,
                answer.getText()
        );

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", "You are a professional job interviewer."));
        messages.add(new ChatMessage("user", prompt));

        String nextQ = llmClient.chat(messages);

        session.getQuestions().add(nextQ);
        return new NextQuestionResponse(nextQ);
    }

    public InterviewFeedback finishInterview(String sessionId) {
        InterviewSession session = sessions.get(sessionId);
        if (session == null) {
            InterviewFeedback fb = new InterviewFeedback();
            fb.setOverallSummary("Session not found.");
            fb.setScores(Map.of());
            fb.setStrengths(List.of());
            fb.setAreasToImprove(List.of());
            fb.setSampleImprovedAnswers(List.of());
            return fb;
        }

        session.setFinished(true);

        StringBuilder convo = new StringBuilder();
        for (int i = 0; i < session.getQuestions().size(); i++) {
            convo.append("Q").append(i + 1).append(": ")
                    .append(session.getQuestions().get(i)).append("\n");
            if (i < session.getAnswers().size()) {
                convo.append("A").append(i + 1).append(": ")
                        .append(session.getAnswers().get(i)).append("\n\n");
            }
        }

        String prompt = String.format("""
                You are an interview coach. Evaluate this mock interview.

                Role: %s
                Experience level: %s
                Interview type: %s

                Conversation:
                %s

                Respond ONLY in strict JSON with the following schema.
                Do NOT wrap it in ```json or any markdown fences.

                {
                  "overallSummary": "string",
                  "scores": {
                    "communication": 0-10,
                    "technicalDepth": 0-10,
                    "structure": 0-10,
                    "confidence": 0-10
                  },
                  "strengths": ["string"],
                  "areasToImprove": ["string"],
                  "sampleImprovedAnswers": [
                    {
                      "question": "string",
                      "improvedAnswer": "string"
                    }
                  ]
                }
                """,
                session.getRole(),
                session.getExperienceLevel(),
                session.getInterviewType(),
                convo
        );

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", "You are an interview coach providing structured feedback."));
        messages.add(new ChatMessage("user", prompt));

        String raw = llmClient.chat(messages);

        InterviewFeedback feedback = new InterviewFeedback();

        try {
            // 🔹 1. Clean possible ```json ... ``` wrapping from the LLM
            String cleaned = raw.trim();

            if (cleaned.startsWith("```")) {
                int firstBrace = cleaned.indexOf('{');
                int lastBrace = cleaned.lastIndexOf('}');
                if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                    cleaned = cleaned.substring(firstBrace, lastBrace + 1);
                }
            }

            // 🔹 2. Parse cleaned JSON
            JsonNode root = objectMapper.readTree(cleaned);

            JsonNode summaryNode = root.get("overallSummary");
            feedback.setOverallSummary(
                    summaryNode != null && !summaryNode.isNull()
                            ? summaryNode.asText()
                            : cleaned
            );

            JsonNode scoresNode = root.get("scores");
            if (scoresNode != null && scoresNode.isObject()) {
                Map<String, Integer> scores = new LinkedHashMap<>();
                Iterator<String> fieldNames = scoresNode.fieldNames();
                while (fieldNames.hasNext()) {
                    String name = fieldNames.next();
                    scores.put(name, scoresNode.get(name).asInt());
                }
                feedback.setScores(scores);
            } else {
                feedback.setScores(Map.of());
            }

            JsonNode strengthsNode = root.get("strengths");
            if (strengthsNode != null && strengthsNode.isArray()) {
                List<String> strengths = new ArrayList<>();
                for (JsonNode n : strengthsNode) {
                    strengths.add(n.asText());
                }
                feedback.setStrengths(strengths);
            } else {
                feedback.setStrengths(List.of());
            }

            JsonNode areasNode = root.get("areasToImprove");
            if (areasNode != null && areasNode.isArray()) {
                List<String> areas = new ArrayList<>();
                for (JsonNode n : areasNode) {
                    areas.add(n.asText());
                }
                feedback.setAreasToImprove(areas);
            } else {
                feedback.setAreasToImprove(List.of());
            }

            JsonNode samplesNode = root.get("sampleImprovedAnswers");
            if (samplesNode != null && samplesNode.isArray()) {
                List<InterviewFeedback.SampleImprovedAnswer> samples = new ArrayList<>();
                for (JsonNode n : samplesNode) {
                    InterviewFeedback.SampleImprovedAnswer s = new InterviewFeedback.SampleImprovedAnswer();
                    JsonNode qNode = n.get("question");
                    JsonNode aNode = n.get("improvedAnswer");
                    s.setQuestion(qNode != null ? qNode.asText() : "");
                    s.setImprovedAnswer(aNode != null ? aNode.asText() : "");
                    samples.add(s);
                }
                feedback.setSampleImprovedAnswers(samples);
            } else {
                feedback.setSampleImprovedAnswers(List.of());
            }

        } catch (Exception e) {
            // If anything goes wrong, at least show the raw text
            feedback.setOverallSummary(raw);
            feedback.setScores(Map.of());
            feedback.setStrengths(List.of());
            feedback.setAreasToImprove(List.of());
            feedback.setSampleImprovedAnswers(List.of());
        }

        return feedback;
    }
}
