package com.leadproject.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConversationState {

    private static final int MAX_FACTS = 20;
    private static final int MAX_CORRECTIONS = 10;
    private static final double MIN_CONFIRMED_FACT_CONFIDENCE = 0.7;

    private int turnCount;
    private Map<String, Fact> facts = new LinkedHashMap<>();
    private List<Correction> corrections = new ArrayList<>();

    public int getTurnCount() { return turnCount; }
    public void setTurnCount(int turnCount) { this.turnCount = turnCount; }
    public Map<String, Fact> getFacts() { return facts; }
    public void setFacts(Map<String, Fact> facts) { this.facts = facts; }
    public List<Correction> getCorrections() { return corrections; }
    public void setCorrections(List<Correction> corrections) { this.corrections = corrections; }

    public void apply(List<FactUpdate> updates) {
        if (facts == null) {
            facts = new LinkedHashMap<>();
        }
        if (corrections == null) {
            corrections = new ArrayList<>();
        }
        turnCount++;
        if (updates == null) {
            return;
        }

        for (FactUpdate update : updates) {
            if (!isValid(update)) {
                continue;
            }
            String key = update.getKey().trim().toLowerCase(Locale.ROOT);
            String value = update.getValue().trim();
                String status = "CONFIRMED".equalsIgnoreCase(update.getStatus())
                    && update.getConfidence() >= MIN_CONFIRMED_FACT_CONFIDENCE
                    ? "CONFIRMED"
                    : "TENTATIVE";
            Fact previous = facts.get(key);

            if (previous == null) {
                if (facts.size() >= MAX_FACTS) {
                    continue;
                }
                facts.put(key, new Fact(value, status, update.getConfidence(), update.getEvidence(), turnCount));
                continue;
            }

            if (previous.getValue().equalsIgnoreCase(value)) {
                if ("CONFIRMED".equals(status)) {
                    previous.setStatus(status);
                }
                previous.setConfidence(Math.max(previous.getConfidence(), update.getConfidence()));
                previous.setEvidence(nonBlank(update.getEvidence(), previous.getEvidence()));
                previous.setTurnIndex(turnCount);
                continue;
            }

            if ("CORRECTION".equalsIgnoreCase(update.getUpdateType()) && "CONFIRMED".equals(status)) {
                corrections.add(new Correction(key, previous.getValue(), value,
                        update.getEvidence(), turnCount));
                facts.put(key, new Fact(value, status, update.getConfidence(), update.getEvidence(), turnCount));
            } else if ("TENTATIVE".equals(previous.getStatus()) && "CONFIRMED".equals(status)) {
                facts.put(key, new Fact(value, status, update.getConfidence(), update.getEvidence(), turnCount));
            }
        }

        if (corrections.size() > MAX_CORRECTIONS) {
            corrections = new ArrayList<>(corrections.subList(corrections.size() - MAX_CORRECTIONS, corrections.size()));
        }
    }

    private boolean isValid(FactUpdate update) {
        if (update == null || update.getKey() == null || update.getValue() == null
                || update.getKey().isBlank() || update.getValue().isBlank()) {
            return false;
        }
        String key = update.getKey().trim();
        double confidence = update.getConfidence();
        return key.length() <= 64 && key.matches("[A-Za-z][A-Za-z0-9_.-]*")
            && update.getValue().trim().length() <= 250
            && (update.getEvidence() == null || update.getEvidence().length() <= 160)
                && Double.isFinite(confidence) && confidence >= 0 && confidence <= 1;
    }

    private String nonBlank(String candidate, String fallback) {
        return candidate == null || candidate.isBlank() ? fallback : candidate;
    }

    public static class FactUpdate {
        private String key;
        private String value;
        private String status;
        private double confidence;
        private String evidence;
        private String updateType;

        public FactUpdate() { }

        public FactUpdate(String key, String value, String status, double confidence,
                          String evidence, String updateType) {
            this.key = key;
            this.value = value;
            this.status = status;
            this.confidence = confidence;
            this.evidence = evidence;
            this.updateType = updateType;
        }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public String getEvidence() { return evidence; }
        public void setEvidence(String evidence) { this.evidence = evidence; }
        public String getUpdateType() { return updateType; }
        public void setUpdateType(String updateType) { this.updateType = updateType; }
    }

    public static class Fact {
        private String value;
        private String status;
        private double confidence;
        private String evidence;
        private int turnIndex;

        public Fact() { }

        public Fact(String value, String status, double confidence, String evidence, int turnIndex) {
            this.value = value;
            this.status = status;
            this.confidence = confidence;
            this.evidence = evidence;
            this.turnIndex = turnIndex;
        }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public String getEvidence() { return evidence; }
        public void setEvidence(String evidence) { this.evidence = evidence; }
        public int getTurnIndex() { return turnIndex; }
        public void setTurnIndex(int turnIndex) { this.turnIndex = turnIndex; }
    }

    public static class Correction {
        private String key;
        private String previousValue;
        private String correctedValue;
        private String evidence;
        private int turnIndex;

        public Correction() { }

        public Correction(String key, String previousValue, String correctedValue,
                          String evidence, int turnIndex) {
            this.key = key;
            this.previousValue = previousValue;
            this.correctedValue = correctedValue;
            this.evidence = evidence;
            this.turnIndex = turnIndex;
        }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getPreviousValue() { return previousValue; }
        public void setPreviousValue(String previousValue) { this.previousValue = previousValue; }
        public String getCorrectedValue() { return correctedValue; }
        public void setCorrectedValue(String correctedValue) { this.correctedValue = correctedValue; }
        public String getEvidence() { return evidence; }
        public void setEvidence(String evidence) { this.evidence = evidence; }
        public int getTurnIndex() { return turnIndex; }
        public void setTurnIndex(int turnIndex) { this.turnIndex = turnIndex; }
    }
}