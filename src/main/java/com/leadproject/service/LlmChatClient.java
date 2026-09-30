package com.leadproject.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.leadproject.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class LlmChatClient {

    private static final Logger logger = LoggerFactory.getLogger(LlmChatClient.class);

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public LlmChatClient(AiProperties aiProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(aiProperties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(aiProperties.getReadTimeoutMs());
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
        logger.info("LLM client configured: provider={}, chatUrl={}, model={}",
                aiProperties.normalizedProvider(), chatUrl(), model());
    }

    public String complete(String systemPrompt, String userPrompt) {
        return switch (aiProperties.normalizedProvider()) {
            case "openai" -> openAiCompatible(
                    aiProperties.getOpenai().getBaseUrl(),
                    requirePath(aiProperties.getOpenai().getChatPath(), "app.ai.openai.chat-path"),
                    aiProperties.getOpenai().getApiKey(),
                    aiProperties.getOpenai().getModel(),
                    systemPrompt,
                    userPrompt,
                    null,
                    null);
            case "openrouter" -> openAiCompatible(
                    aiProperties.getOpenrouter().getBaseUrl(),
                    requirePath(aiProperties.getOpenrouter().getChatPath(), "app.ai.openrouter.chat-path"),
                    aiProperties.getOpenrouter().getApiKey(),
                    aiProperties.getOpenrouter().getModel(),
                    systemPrompt,
                    userPrompt,
                    aiProperties.getOpenrouter().getHttpReferer(),
                    aiProperties.getOpenrouter().getAppTitle());
            case "anthropic", "claude" -> anthropic(systemPrompt, userPrompt);
            case "ollama" -> ollama(systemPrompt, userPrompt);
            default -> throw new IllegalStateException(
                    "Unsupported AI provider '" + aiProperties.getProvider()
                            + "'. Use ollama, openai, openrouter, or anthropic.");
        };
    }

    public String chatUrl() {
        return switch (aiProperties.normalizedProvider()) {
            case "openai" -> join(aiProperties.getOpenai().getBaseUrl(),
                    requirePath(aiProperties.getOpenai().getChatPath(), "app.ai.openai.chat-path"));
            case "openrouter" -> join(aiProperties.getOpenrouter().getBaseUrl(),
                    requirePath(aiProperties.getOpenrouter().getChatPath(), "app.ai.openrouter.chat-path"));
            case "anthropic", "claude" -> join(aiProperties.getAnthropic().getBaseUrl(),
                    requirePath(aiProperties.getAnthropic().getMessagesPath(), "app.ai.anthropic.messages-path"));
            default -> join(aiProperties.getOllama().getBaseUrl(),
                    requirePath(aiProperties.getOllama().getChatPath(), "app.ai.ollama.chat-path"));
        };
    }

    public String model() {
        return switch (aiProperties.normalizedProvider()) {
            case "openai" -> aiProperties.getOpenai().getModel();
            case "openrouter" -> aiProperties.getOpenrouter().getModel();
            case "anthropic", "claude" -> aiProperties.getAnthropic().getModel();
            default -> aiProperties.getOllama().getModel();
        };
    }

    public String provider() {
        return aiProperties.normalizedProvider();
    }

    public boolean supportsWarmup() {
        return "ollama".equals(aiProperties.normalizedProvider());
    }

    private String ollama(String systemPrompt, String userPrompt) {
        try {
            AiProperties.Ollama ollama = aiProperties.getOllama();
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", ollama.getModel());
            body.put("stream", false);
            body.put("keep_alive", ollama.getKeepAlive());

            ArrayNode messages = body.putArray("messages");
            messages.addObject().put("role", "system").put("content", systemPrompt);
            messages.addObject().put("role", "user").put("content", userPrompt);

            ObjectNode options = body.putObject("options");
            options.put("num_predict", ollama.getNumPredict() > 0 ? ollama.getNumPredict() : aiProperties.getMaxTokens());
            options.put("num_ctx", ollama.getNumCtx());
            options.put("temperature", aiProperties.getTemperature());

            String raw = restClient.post()
                    .uri(chatUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .body(String.class);

            JsonNode content = objectMapper.readTree(raw).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new IllegalStateException("Ollama returned an empty response");
            }
            return content.asText().trim();
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to call Ollama at " + chatUrl(), exception);
        }
    }

    private String openAiCompatible(
            String baseUrl,
            String chatPath,
            String apiKey,
            String model,
            String systemPrompt,
            String userPrompt,
            String httpReferer,
            String appTitle) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "API key is required for provider '" + provider()
                            + "'. Set OPENAI_API_KEY or OPENROUTER_API_KEY.");
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("temperature", aiProperties.getTemperature());
            body.put("max_tokens", aiProperties.getMaxTokens());

            ArrayNode messages = body.putArray("messages");
            messages.addObject().put("role", "system").put("content", systemPrompt);
            messages.addObject().put("role", "user").put("content", userPrompt);

            String url = join(baseUrl, chatPath);
            var request = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey);
            if (httpReferer != null && !httpReferer.isBlank()) {
                request = request.header("HTTP-Referer", httpReferer);
            }
            if (appTitle != null && !appTitle.isBlank()) {
                request = request.header("X-Title", appTitle);
            }

            String raw = request
                    .body(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .body(String.class);

            JsonNode content = objectMapper.readTree(raw).path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new IllegalStateException(provider() + " returned an empty response");
            }
            return content.asText().trim();
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to call " + provider() + " chat API", exception);
        }
    }

    private String anthropic(String systemPrompt, String userPrompt) {
        AiProperties.Anthropic anthropic = aiProperties.getAnthropic();
        if (anthropic.getApiKey() == null || anthropic.getApiKey().isBlank()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY is required when app.ai.provider=anthropic");
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", anthropic.getModel());
            body.put("max_tokens", aiProperties.getMaxTokens());
            body.put("temperature", aiProperties.getTemperature());
            body.put("system", systemPrompt);

            ArrayNode messages = body.putArray("messages");
            messages.addObject().put("role", "user").put("content", userPrompt);

            String raw = restClient.post()
                    .uri(chatUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-api-key", anthropic.getApiKey())
                    .header("anthropic-version", anthropic.getVersion())
                    .body(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .body(String.class);

            JsonNode content = objectMapper.readTree(raw).path("content").path(0).path("text");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new IllegalStateException("Anthropic returned an empty response");
            }
            return content.asText().trim();
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to call Anthropic at " + chatUrl(), exception);
        }
    }


    private static String requirePath(String configured, String key) {
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException(key + " must be configured in application.yml");
        }
        return configured;
    }

    private static String join(String base, String path) {
        String b = base == null ? "" : base.trim();
        if (b.endsWith("/")) {
            b = b.substring(0, b.length() - 1);
        }
        if (b.endsWith("/v1") && path.startsWith("/v1/")) {
            return b + path.substring(3);
        }
        return b + path;
    }
}
