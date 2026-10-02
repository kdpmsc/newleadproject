package com.leadproject.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@ConfigurationProperties(prefix = "app.voice")
public class VoiceScriptProperties {

    private String companyName;
    private String greetingTemplate;
    private String ivGreetingTemplate;
    /** 1-based IVR questions Q1..Qn; defaults live in application.yml. */
    private List<String> ivQuestions = new ArrayList<>();
    private String noAnswerPhrase;
    private String unavailablePhrase;
    private String optOutPhrase;
    private String closingPhrase;
    private long thinkBudgetMs;
    private int maxThinkPolls;
    private int thinkPauseSeconds;
    private String speechLanguage;
    private String speechVoice;
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