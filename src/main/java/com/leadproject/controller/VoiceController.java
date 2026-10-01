package com.leadproject.controller;

import com.leadproject.service.LeadService;
import com.leadproject.service.LeadCallService;
import com.leadproject.service.AiVoiceAgentService;
import com.leadproject.config.AppProperties;
import com.leadproject.config.VoiceScriptProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/v1")
public class VoiceController {

    private static final Logger logger = LoggerFactory.getLogger(VoiceController.class);

    private final LeadService leadService;
    private final LeadCallService leadCallService;
    private final AiVoiceAgentService aiVoiceAgentService;
    private final AppProperties appProperties;
    private final VoiceScriptProperties voiceScript;
    private final ConcurrentHashMap<String, PendingAiTurn> pendingAiTurns = new ConcurrentHashMap<>();

    public VoiceController(LeadService leadService, LeadCallService leadCallService,
                           AiVoiceAgentService aiVoiceAgentService,
                           AppProperties appProperties,
                           VoiceScriptProperties voiceScript) {
        this.leadService = leadService;
        this.leadCallService = leadCallService;
        this.aiVoiceAgentService = aiVoiceAgentService;
        this.appProperties = appProperties;
        this.voiceScript = voiceScript;
    }

    @RequestMapping(value = "/voice/property-qualification",
            method = {RequestMethod.GET, RequestMethod.POST},
            produces = MediaType.TEXT_XML_VALUE)
    public String propertyQualificationTwiml(
            @RequestParam(required = false) Long leadId,
            @RequestParam(required = false) String leadName,
            HttpServletRequest request) {
        logger.info("Voice qualification request: leadId={}, leadNamePresent={}",
            leadId, leadName != null && !leadName.isBlank());
        return buildQualificationTwiml(leadId, leadName, publicBaseUrl(request));
    }

    @RequestMapping(value = "/voice/inbound",
            method = {RequestMethod.GET, RequestMethod.POST},
            produces = MediaType.TEXT_XML_VALUE)
    public String inboundCallTwiml(
            @RequestParam(required = false) Long leadId,
            @RequestParam(required = false) String leadName,
            HttpServletRequest request) {
        logger.info("Inbound voice request: leadId={}, leadNamePresent={}",
            leadId, leadName != null && !leadName.isBlank());
        return buildQualificationTwiml(leadId, leadName, publicBaseUrl(request));
    }

    @RequestMapping(value = "/voice/ai-agent",
            method = {RequestMethod.GET, RequestMethod.POST},
            produces = MediaType.TEXT_XML_VALUE)
    public String startAiAgent(
            @RequestParam Long leadId,
            @RequestParam(required = false) String leadName,
            @RequestParam(required = false) String CallSid,
            HttpServletRequest request) {
        logger.info("AI voice start request: leadId={}, callSid={}, leadNamePresent={}",
                leadId, CallSid, leadName != null && !leadName.isBlank());
        // Return TwiML immediately; call the LLM only after the caller speaks (/voice/ai-agent/turn).
        // Warm the model in the background while the greeting plays / caller speaks (Ollama only).
        aiVoiceAgentService.warmupAsync();
        return buildAiAgentGreetingTwiml(leadId, leadName, CallSid, publicBaseUrl(request));
    }

    @PostMapping(value = "/voice/ai-agent/turn", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_XML_VALUE)
    public String handleAiAgentTurn(
            @RequestParam Long leadId,
            @RequestParam(required = false) String leadName,
            @RequestParam(required = false, defaultValue = "") String SpeechResult,
            @RequestParam(required = false) String CallSid,
            HttpServletRequest request) {
        logger.info("AI voice turn request: leadId={}, callSid={}, speechLength={}",
                leadId, CallSid, SpeechResult == null ? 0 : SpeechResult.length());
        return beginAiTurn(leadId, leadName, SpeechResult, CallSid, publicBaseUrl(request));
    }

    /**
     * Twilio Redirect target while the LLM generates. Returns immediately with Pause+Redirect
     * until the async reply is ready, so Twilio's ~15s webhook limit is not breached.
     */
    @RequestMapping(value = "/voice/ai-agent/think",
            method = {RequestMethod.GET, RequestMethod.POST},
            produces = MediaType.TEXT_XML_VALUE)
    public String handleAiAgentThink(
            @RequestParam Long leadId,
            @RequestParam(required = false) String leadName,
            @RequestParam(required = false) String CallSid,
            @RequestParam(required = false) String turnKey,
            HttpServletRequest request) {
        String key = (turnKey == null || turnKey.isBlank()) ? turnKey(leadId, CallSid) : turnKey;
        logger.info("AI voice think poll: leadId={}, callSid={}, turnKey={}", leadId, CallSid, key);
        return pollAiTurn(leadId, leadName, CallSid, key, publicBaseUrl(request));
    }

    @PostMapping(value = "/voice/status", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> handleVoiceStatus(@RequestParam(required = false) String CallSid,
                                                @RequestParam(required = false) String CallUUID,
                                                @RequestParam(required = false) String CallStatus,
                                                @RequestParam(required = false) String CallDuration,
                                                @RequestParam(required = false) String To,
                                                @RequestParam(required = false) String From) {
                            logger.info("Voice status callback: callSid={}, callUuid={}, status={}, toPresent={}, fromPresent={}",
                                CallSid, CallUUID, CallStatus, To != null && !To.isBlank(), From != null && !From.isBlank());
        Long durationSeconds = null;
        if (CallDuration != null && !CallDuration.isBlank()) {
            try {
                durationSeconds = Long.valueOf(CallDuration);
            } catch (NumberFormatException exception) {
                logger.warn("Ignoring invalid call duration: callSid={}, duration={}", CallSid, CallDuration);
            }
        }
        leadCallService.updateProviderStatus(CallSid != null ? CallSid : CallUUID, CallStatus, durationSeconds);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/voice/answer", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_XML_VALUE)
    public String handleAnswer(@RequestParam Long leadId,
                               @RequestParam int question,
                       @RequestParam(required = false, defaultValue = "") String SpeechResult,
                               @RequestParam(required = false) String CallSid,
                               HttpServletRequest request) {
                    logger.info("Qualification answer callback: leadId={}, question={}, callSid={}, speechLength={}",
                        leadId, question, CallSid, SpeechResult == null ? 0 : SpeechResult.length());
        return processAnswer(leadId, question, SpeechResult, CallSid, publicBaseUrl(request));
    }

    @PostMapping(value = "/voice/answer", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_XML_VALUE)
    public String handleJsonAnswer(@RequestBody java.util.Map<String, Object> payload,
                                   HttpServletRequest request) {
        logger.info("Qualification JSON callback received: fields={}", payload.keySet());
        Long leadId = Long.valueOf(String.valueOf(payload.get("leadId")));
        int question = Integer.parseInt(String.valueOf(payload.get("question")));
        String speechResult = String.valueOf(payload.getOrDefault("SpeechResult", ""));
        String callSid = payload.get("CallSid") == null ? null : String.valueOf(payload.get("CallSid"));
        return processAnswer(leadId, question, speechResult, callSid, publicBaseUrl(request));
    }

    private String processAnswer(Long leadId, int question, String speechResult, String callSid, String publicBaseUrl) {
        logger.info("Processing qualification answer: leadId={}, question={}, callSid={}",
            leadId, question, callSid);
        leadCallService.appendAnswer(leadId, question, speechResult, callSid);

        int next = question + 1;
        if (voiceScript.getIvQuestions() != null && next <= voiceScript.getIvQuestions().size()) {
            return nextQuestion(leadId, next, voiceScript.getIvQuestion(next), publicBaseUrl);
        }
        return "<Response><Say language=\"%s\">%s</Say></Response>".formatted(
                escapeXmlAttribute(voiceScript.getSpeechLanguage()),
                escapeXml(voiceScript.getClosingPhrase()));
    }

    private String beginAiTurn(Long leadId, String leadName, String speechResult, String callSid, String publicBaseUrl) {
        long startedAt = System.nanoTime();
        logger.info("Processing AI voice turn: leadId={}, callSid={}, speechLength={}",
            leadId, callSid, speechResult == null ? 0 : speechResult.length());

        if (isOptOut(speechResult)) {
            if (speechResult != null && !speechResult.isBlank()) {
                leadCallService.appendAgentTurn(leadId, callSid, "user", speechResult);
            }
            return "<Response><Say language=\"%s\">%s</Say></Response>".formatted(escapeXmlAttribute(voiceScript.getSpeechLanguage()), escapeXml(voiceScript.getOptOutPhrase()));
        }

        if (speechResult != null && !speechResult.isBlank()) {
            leadCallService.appendAgentTurn(leadId, callSid, "user", speechResult);
        }

        String conversation = leadCallService.getConversation(leadId, callSid);
        String key = turnKey(leadId, callSid);
        final String resolvedLeadName = leadName;
        final String resolvedCallSid = callSid;

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() ->
                aiVoiceAgentService.respond(resolvedLeadName, conversation));
        pendingAiTurns.put(key, new PendingAiTurn(future, System.currentTimeMillis(), new AtomicInteger(0), speechResult));

        logger.info("AI voice turn queued async: leadId={}, callSid={}, turnKey={}, durationMs={}",
                leadId, callSid, key, elapsedMillis(startedAt));
        return thinkingTwiml(leadId, leadName, callSid, key, publicBaseUrl, true);
    }

    private String pollAiTurn(Long leadId, String leadName, String callSid, String key, String publicBaseUrl) {
        PendingAiTurn pending = pendingAiTurns.get(key);
        if (pending == null) {
            logger.warn("AI voice think missing pending turn: leadId={}, callSid={}, turnKey={}", leadId, callSid, key);
            return unavailableTwiml();
        }

        int poll = pending.polls.incrementAndGet();
        long waitedMs = System.currentTimeMillis() - pending.startedAtMs;

        if (pending.future.isDone()) {
            pendingAiTurns.remove(key);
            try {
                String response = pending.future.get(1, TimeUnit.SECONDS);
                leadCallService.appendAgentTurn(leadId, callSid, "assistant", response);
                logger.info("AI voice turn completed: leadId={}, callSid={}, responseLength={}, waitedMs={}, polls={}",
                        leadId, callSid, response.length(), waitedMs, poll);
                return aiReplyTwiml(leadId, leadName, response, publicBaseUrl);
            } catch (Exception exception) {
                logger.error("AI voice agent unavailable: leadId={}, callSid={}, provider={}, url={}, model={}, waitedMs={}",
                        leadId, callSid, aiVoiceAgentService.getProvider(), aiVoiceAgentService.getChatUrl(), aiVoiceAgentService.getModel(),
                        waitedMs, exception);
                return unavailableTwiml();
            }
        }

        if (poll > voiceScript.getMaxThinkPolls() || waitedMs > voiceScript.getThinkBudgetMs()) {
            pendingAiTurns.remove(key);
            pending.future.cancel(true);
            logger.warn("AI voice turn timed out waiting for LLM: leadId={}, callSid={}, waitedMs={}, polls={}",
                    leadId, callSid, waitedMs, poll);
            return unavailableTwiml();
        }

        logger.info("AI voice think waiting: leadId={}, callSid={}, waitedMs={}, poll={}",
                leadId, callSid, waitedMs, poll);
        return thinkingTwiml(leadId, leadName, callSid, key, publicBaseUrl, false);
    }

    private String thinkingTwiml(Long leadId, String leadName, String callSid, String turnKey,
                                 String publicBaseUrl, boolean first) {
        String thinkUrl = publicBaseUrl + "/api/v1/voice/ai-agent/think?leadId=" + leadId
                + "&turnKey=" + urlEncode(turnKey);
        if (leadName != null && !leadName.isBlank()) {
            thinkUrl += "&leadName=" + urlEncode(leadName);
        }
        if (first) {
            return """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Response>
                        <Redirect method="POST">%s</Redirect>
                    </Response>
                    """.formatted(escapeXml(thinkUrl));
        }
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Response>
                    <Pause length="%d"/>
                    <Redirect method="POST">%s</Redirect>
                </Response>
                """.formatted(voiceScript.getThinkPauseSeconds(), escapeXml(thinkUrl));
    }

    private String aiReplyTwiml(Long leadId, String leadName, String response, String publicBaseUrl) {
        String action = publicBaseUrl + "/api/v1/voice/ai-agent/turn?leadId=" + leadId;
        if (leadName != null && !leadName.isBlank()) {
            action += "&leadName=" + urlEncode(leadName);
        }
        String lang = escapeXmlAttribute(voiceScript.getSpeechLanguage());
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Response>
                    <Gather input="speech" action="%s" method="POST" language="%s" speechTimeout="auto">
                        <Say language="%s">%s</Say>
                    </Gather>
                    <Say language="%s">%s</Say>
                </Response>
                """.formatted(escapeXmlAttribute(action), lang, lang, escapeXml(response),
                lang, escapeXml(voiceScript.getNoAnswerPhrase()));
    }

    private String unavailableTwiml() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Response>
                    <Say language="%s">%s</Say>
                </Response>
                """.formatted(escapeXmlAttribute(voiceScript.getSpeechLanguage()),
                escapeXml(voiceScript.getUnavailablePhrase()));
    }

    private boolean isOptOut(String speechResult) {
        return voiceScript.matchesOptOut(speechResult);
    }

    @PostMapping(value = "/voice/recording", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> handleRecording(@RequestParam String CallSid,
                                                @RequestParam(required = false) String RecordingSid,
                                                @RequestParam(required = false) String RecordingUrl,
                                                @RequestParam(required = false) String RecordingStatus) {
        logger.info("Recording callback: callSid={}, recordingSid={}, status={}, urlPresent={}",
            CallSid, RecordingSid, RecordingStatus, RecordingUrl != null && !RecordingUrl.isBlank());
        leadCallService.updateRecording(CallSid, RecordingSid, RecordingUrl, RecordingStatus);
        return ResponseEntity.ok().build();
    }

    private String buildAiAgentGreetingTwiml(Long leadId, String leadName, String callSid, String publicBaseUrl) {
        String spokenGreeting = voiceScript.formatGreeting(leadName);
        leadCallService.appendAgentTurn(leadId, callSid, "assistant", spokenGreeting);

        String action = publicBaseUrl + "/api/v1/voice/ai-agent/turn?leadId=" + leadId;
        if (leadName != null && !leadName.isBlank()) {
            action += "&leadName=" + urlEncode(leadName);
        }
        String lang = escapeXmlAttribute(voiceScript.getSpeechLanguage());
        logger.info("AI voice greeting ready: leadId={}, callSid={}", leadId, callSid);
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Response>
                    <Gather input="speech" action="%s" method="POST" language="%s" speechTimeout="auto">
                        <Say language="%s">%s</Say>
                    </Gather>
                    <Say language="%s">%s</Say>
                </Response>
                """.formatted(escapeXmlAttribute(action), lang, lang, escapeXml(spokenGreeting),
                lang, escapeXml(voiceScript.getNoAnswerPhrase()));
    }

    private String buildQualificationTwiml(Long leadId, String leadName, String publicBaseUrl) {
        String greeting = voiceScript.formatIvGreeting(leadName == null || leadName.isBlank() ? voiceScript.getFallbackLeadName() : leadName);
        String action = publicBaseUrl + "/api/v1/voice/answer?leadId=" + leadId + "&question=1";
        String lang = escapeXmlAttribute(voiceScript.getSpeechLanguage());
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Response>
                    <Say language="%s">%s</Say>
                    <Gather input="speech" action="%s" method="POST" language="%s" speechTimeout="auto">
                        <Say language="%s">%s</Say>
                    </Gather>
                    <Say language="%s">%s</Say>
                </Response>
                """.formatted(lang, escapeXml(greeting), escapeXmlAttribute(action), lang, lang,
                escapeXml(voiceScript.getIvFirstQuestion()), lang, escapeXml(voiceScript.getNoAnswerPhrase()));
    }

    private String nextQuestion(Long leadId, int question, String text, String publicBaseUrl) {
        String action = publicBaseUrl + "/api/v1/voice/answer?leadId=" + leadId + "&question=" + question;
        String lang = escapeXmlAttribute(voiceScript.getSpeechLanguage());
        return """
                <Response>
                    <Gather input="speech" action="%s" method="POST" language="%s" speechTimeout="auto">
                        <Say language="%s">%s</Say>
                    </Gather>
                    <Say language="%s">%s</Say>
                </Response>
                """.formatted(escapeXmlAttribute(action), lang, lang, escapeXml(text),
                lang, escapeXml(voiceScript.getNoAnswerPhrase()));
    }

    private String turnKey(Long leadId, String callSid) {
        if (callSid != null && !callSid.isBlank()) {
            return callSid;
        }
        return "lead-" + leadId + "-" + System.currentTimeMillis();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String escapeXmlAttribute(String value) {
        return escapeXml(value).replace("\"", "&quot;");
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private String publicBaseUrl(HttpServletRequest request) {
        if (request != null) {
            String host = firstHeader(request, "X-Forwarded-Host");
            String proto = firstHeader(request, "X-Forwarded-Proto");
            if (host == null || host.isBlank()) {
                host = request.getServerName();
                int port = request.getServerPort();
                boolean defaultPort = port == 80 || port == 443;
                if (!defaultPort && proto == null) {
                    host = host + ":" + port;
                }
            }
            if (proto == null || proto.isBlank()) {
                proto = request.isSecure() ? "https" : "http";
            }
            if (host != null && !host.isBlank()) {
                return proto + "://" + host;
            }
        }
        return appProperties.trimmedPublicBaseUrl();
    }

    private String firstHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.split(",")[0].trim();
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static final class PendingAiTurn {
        private final CompletableFuture<String> future;
        private final long startedAtMs;
        private final AtomicInteger polls;
        private final String speechResult;

        private PendingAiTurn(CompletableFuture<String> future, long startedAtMs, AtomicInteger polls, String speechResult) {
            this.future = future;
            this.startedAtMs = startedAtMs;
            this.polls = polls;
            this.speechResult = speechResult;
        }
    }
}
