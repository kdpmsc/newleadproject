package com.leadproject.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@ConfigurationProperties(prefix = "app.voice")
public class VoiceScriptProperties {

    private static final Set<String> NON_PROGRESS_TOKENS = Set.of(
            "hello", "hi", "hey", "anyone", "there", "are", "you", "still", "listening",
            "can", "hear", "me", "please", "sorry");

    private String companyName;
    private String greetingTemplate;
    private String ivGreetingTemplate;
    /** 1-based IVR questions Q1..Qn; defaults live in application.yml. */
    private List<String> ivQuestions = new ArrayList<>();
    private String noAnswerPhrase;
    private String unavailablePhrase;
    private String optOutPhrase;
    private String closingPhrase;
    private String conversationEndPhrase;
    private List<String> conversationEndKeywords = new ArrayList<>();
    private int maxConsecutiveNoProgressTurns;
    private long thinkBudgetMs;
    private int maxThinkPolls;
    private int thinkPauseSeconds;
    private String speechLanguage;
    private String speechVoice;
    private String speechRecognitionHints;
    private double speechConfidenceThreshold;
    private int maxSpeechClarificationRetries;
    private String speechClarificationPhrase;
    private List<String> optOutKeywords = new ArrayList<>();
    private String scriptIntro;
    private String summaryTemplate;
    private String nextAction;
    private String fallbackLeadName;
    private String fallbackCompanyName;

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getGreetingTemplate() { return greetingTemplate; }
    public void setGreetingTemplate(String greetingTemplate) { this.greetingTemplate = greetingTemplate; }
    public String getIvGreetingTemplate() { return ivGreetingTemplate; }
    public void setIvGreetingTemplate(String ivGreetingTemplate) { this.ivGreetingTemplate = ivGreetingTemplate; }
    public List<String> getIvQuestions() { return ivQuestions; }
    public void setIvQuestions(List<String> ivQuestions) { this.ivQuestions = ivQuestions; }
    public String getNoAnswerPhrase() { return noAnswerPhrase; }
    public void setNoAnswerPhrase(String noAnswerPhrase) { this.noAnswerPhrase = noAnswerPhrase; }
    public String getUnavailablePhrase() { return unavailablePhrase; }
    public void setUnavailablePhrase(String unavailablePhrase) { this.unavailablePhrase = unavailablePhrase; }
    public String getOptOutPhrase() { return optOutPhrase; }
    public void setOptOutPhrase(String optOutPhrase) { this.optOutPhrase = optOutPhrase; }
    public String getClosingPhrase() { return closingPhrase; }
    public void setClosingPhrase(String closingPhrase) { this.closingPhrase = closingPhrase; }
    public String getConversationEndPhrase() { return conversationEndPhrase; }
    public void setConversationEndPhrase(String conversationEndPhrase) { this.conversationEndPhrase = conversationEndPhrase; }
    public List<String> getConversationEndKeywords() { return conversationEndKeywords; }
    public void setConversationEndKeywords(List<String> conversationEndKeywords) { this.conversationEndKeywords = conversationEndKeywords; }
    public int getMaxConsecutiveNoProgressTurns() { return maxConsecutiveNoProgressTurns; }
    public void setMaxConsecutiveNoProgressTurns(int maxConsecutiveNoProgressTurns) { this.maxConsecutiveNoProgressTurns = maxConsecutiveNoProgressTurns; }
    public long getThinkBudgetMs() { return thinkBudgetMs; }
    public void setThinkBudgetMs(long thinkBudgetMs) { this.thinkBudgetMs = thinkBudgetMs; }
    public int getMaxThinkPolls() { return maxThinkPolls; }
    public void setMaxThinkPolls(int maxThinkPolls) { this.maxThinkPolls = maxThinkPolls; }
    public int getThinkPauseSeconds() { return thinkPauseSeconds; }
    public void setThinkPauseSeconds(int thinkPauseSeconds) { this.thinkPauseSeconds = thinkPauseSeconds; }
    public String getSpeechLanguage() { return speechLanguage; }
    public void setSpeechLanguage(String speechLanguage) { this.speechLanguage = speechLanguage; }
    public String getSpeechVoice() { return speechVoice; }
    public void setSpeechVoice(String speechVoice) { this.speechVoice = speechVoice; }
    public String getSpeechRecognitionHints() { return speechRecognitionHints; }
    public void setSpeechRecognitionHints(String speechRecognitionHints) { this.speechRecognitionHints = speechRecognitionHints; }
    public double getSpeechConfidenceThreshold() { return speechConfidenceThreshold; }
    public void setSpeechConfidenceThreshold(double speechConfidenceThreshold) { this.speechConfidenceThreshold = speechConfidenceThreshold; }
    public int getMaxSpeechClarificationRetries() { return maxSpeechClarificationRetries; }
    public void setMaxSpeechClarificationRetries(int maxSpeechClarificationRetries) { this.maxSpeechClarificationRetries = maxSpeechClarificationRetries; }
    public String getSpeechClarificationPhrase() { return speechClarificationPhrase; }
    public void setSpeechClarificationPhrase(String speechClarificationPhrase) { this.speechClarificationPhrase = speechClarificationPhrase; }
    public List<String> getOptOutKeywords() { return optOutKeywords; }
    public void setOptOutKeywords(List<String> optOutKeywords) { this.optOutKeywords = optOutKeywords; }
    public String getScriptIntro() { return scriptIntro; }
    public void setScriptIntro(String scriptIntro) { this.scriptIntro = scriptIntro; }
    public String getSummaryTemplate() { return summaryTemplate; }
    public void setSummaryTemplate(String summaryTemplate) { this.summaryTemplate = summaryTemplate; }
    public String getNextAction() { return nextAction; }
    public void setNextAction(String nextAction) { this.nextAction = nextAction; }
    public String getFallbackLeadName() { return fallbackLeadName; }
    public void setFallbackLeadName(String fallbackLeadName) { this.fallbackLeadName = fallbackLeadName; }
    public String getFallbackCompanyName() { return fallbackCompanyName; }
    public void setFallbackCompanyName(String fallbackCompanyName) { this.fallbackCompanyName = fallbackCompanyName; }

    /** @param oneBasedIndex question number starting at 1 */
    public String getIvQuestion(int oneBasedIndex) {
        if (ivQuestions == null || oneBasedIndex < 1 || oneBasedIndex > ivQuestions.size()) {
            throw new IllegalStateException(
                    "Missing app.voice.iv-questions entry for question " + oneBasedIndex
                            + " (configured count=" + (ivQuestions == null ? 0 : ivQuestions.size()) + ")");
        }
        return ivQuestions.get(oneBasedIndex - 1);
    }

    public String getIvFirstQuestion() {
        return getIvQuestion(1);
    }

    public boolean matchesOptOut(String speechResult) {
        if (speechResult == null || speechResult.isBlank() || optOutKeywords == null || optOutKeywords.isEmpty()) {
            return false;
        }
        String normalized = speechResult.toLowerCase(Locale.ROOT);
        return optOutKeywords.stream()
                .filter(k -> k != null && !k.isBlank())
                .map(k -> k.toLowerCase(Locale.ROOT))
                .anyMatch(normalized::contains);
    }

    public boolean matchesConversationEnd(String speechResult) {
        if (speechResult == null || speechResult.isBlank()
                || conversationEndKeywords == null || conversationEndKeywords.isEmpty()) {
            return false;
        }
        String normalizedSpeech = normalizePhrase(speechResult);
        return conversationEndKeywords.stream()
                .filter(keyword -> keyword != null && !keyword.isBlank())
                .map(this::normalizePhrase)
                .anyMatch(keyword -> (" " + normalizedSpeech + " ").contains(" " + keyword + " "));
    }

    public boolean isNoProgressSpeech(String speechResult) {
        if (speechResult == null || speechResult.isBlank()) {
            return false;
        }
        String[] tokens = normalizePhrase(speechResult).split("\\s+");
        if (tokens.length == 0 || tokens.length > 8) {
            return false;
        }
        return java.util.Arrays.stream(tokens).allMatch(NON_PROGRESS_TOKENS::contains);
    }

    private String normalizePhrase(String phrase) {
        return phrase.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    public boolean requiresSpeechClarification(String confidence) {
        if (confidence == null || confidence.isBlank()
                || speechConfidenceThreshold <= 0 || speechConfidenceThreshold > 1) {
            return false;
        }
        try {
            double score = Double.parseDouble(confidence);
            return Double.isFinite(score) && score >= 0 && score <= 1 && score < speechConfidenceThreshold;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    public String formatGreeting(String leadName) {
        return apply(greetingTemplate, leadName);
    }

    public String formatIvGreeting(String leadName) {
        return apply(ivGreetingTemplate, leadName);
    }

    private String apply(String template, String leadName) {
        String nameFallback = fallbackLeadName == null || fallbackLeadName.isBlank() ? "there" : fallbackLeadName;
        String companyFallback = fallbackCompanyName == null || fallbackCompanyName.isBlank() ? "" : fallbackCompanyName;
        String name = leadName == null || leadName.isBlank() ? nameFallback : leadName.trim();
        String company = companyName == null || companyName.isBlank() ? companyFallback : companyName;
        String tpl = template == null ? "" : template;
        return tpl.replace("{name}", name).replace("{company}", company);
    }
}