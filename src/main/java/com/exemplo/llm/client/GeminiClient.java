package com.exemplo.llm.client;

import com.exemplo.llm.model.ChatMessage;
import com.exemplo.llm.model.ChatRequest;
import com.exemplo.llm.model.LLMProvider;
import com.exemplo.llm.util.JsonUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Cliente da API {@code generateContent} do Google Gemini.
 *
 * <p>Peculiaridades: roles {@code user|model}, texto em {@code contents[].parts[].text},
 * prompt de sistema em {@code systemInstruction}, API key na URL ({@code ?key=}) e
 * resposta em {@code candidates[0].content.parts[0].text}.
 */
public final class GeminiClient extends AbstractLLMClient {

    private final String apiKey;

    public GeminiClient(HttpClient httpClient, String apiKey, String defaultModel) {
        super(httpClient, defaultModel);
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
    }

    @Override
    public LLMProvider provider() {
        return LLMProvider.GEMINI;
    }

    @Override
    protected HttpRequest buildHttpRequest(ChatRequest request, String model) {
        return jsonPost(endpointFor(model), buildBody(request)).build();
    }

    @Override
    protected String extractContent(String responseBody) {
        return requireContent(JsonUtils.findString(responseBody, "text", "candidates"), responseBody);
    }

    // A chave vai na query string, como na documentação da API. Alternativa: header "x-goog-api-key".
    private URI endpointFor(String model) {
        String encodedModel = URLEncoder.encode(model, StandardCharsets.UTF_8);
        String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        return URI.create("%s/models/%s:generateContent?key=%s"
                .formatted(LLMProvider.GEMINI.baseUrl(), encodedModel, encodedKey));
    }

    private static String buildBody(ChatRequest request) {
        return """
                {
                  %s"contents": [%s],
                  "generationConfig": {"temperature": %s, "maxOutputTokens": %d}
                }
                """.formatted(systemInstructionJson(request), toContentsJson(request),
                request.temperature(), request.maxTokens());
    }

    private static String systemInstructionJson(ChatRequest request) {
        return request.systemPrompt()
                .map(text -> "\"systemInstruction\": {\"parts\": [{\"text\": %s}]},\n  "
                        .formatted(JsonUtils.quote(text)))
                .orElse("");
    }

    private static String toContentsJson(ChatRequest request) {
        return request.conversation().stream()
                .map(GeminiClient::toContentJson)
                .collect(Collectors.joining(", "));
    }

    private static String toContentJson(ChatMessage message) {
        return "{\"role\": %s, \"parts\": [{\"text\": %s}]}".formatted(
                JsonUtils.quote(geminiRole(message.role())), JsonUtils.quote(message.content()));
    }

    private static String geminiRole(ChatMessage.Role role) {
        return switch (role) {
            case USER -> "user";
            case ASSISTANT -> "model";
            case SYSTEM -> throw new IllegalArgumentException(
                    "Mensagens SYSTEM são enviadas em systemInstruction, não em contents");
        };
    }
}
