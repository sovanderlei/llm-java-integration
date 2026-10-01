package com.exemplo.llm.client;

import com.exemplo.llm.model.ChatMessage;
import com.exemplo.llm.model.ChatRequest;
import com.exemplo.llm.model.LLMProvider;
import com.exemplo.llm.util.JsonUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Cliente do endpoint {@code /api/chat} do Ollama (modelos locais).
 *
 * <p>Peculiaridades: sem autenticação, {@code "stream": false} para resposta única,
 * parâmetros de geração em {@code options{}} ({@code num_predict} = max tokens) e
 * resposta em {@code message.content}.
 */
public final class OllamaClient extends AbstractLLMClient {

    // Modelos locais podem demorar bastante na primeira carga.
    private static final Duration LOCAL_TIMEOUT = Duration.ofMinutes(5);

    private final URI baseUrl;
    private final URI endpoint;

    public OllamaClient(HttpClient httpClient, URI baseUrl, String defaultModel) {
        super(httpClient, defaultModel);
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        this.endpoint = URI.create(baseUrl.toString().replaceAll("/+$", "") + "/api/chat");
    }

    @Override
    public LLMProvider provider() {
        return LLMProvider.OLLAMA;
    }

    @Override
    protected HttpRequest buildHttpRequest(ChatRequest request, String model) {
        return jsonPost(endpoint, buildBody(request, model)).build();
    }

    @Override
    protected String extractContent(String responseBody) {
        return requireContent(JsonUtils.findString(responseBody, "content", "message"), responseBody);
    }

    @Override
    protected Duration requestTimeout() {
        return LOCAL_TIMEOUT;
    }

    @Override
    protected String connectionHint() {
        return "O Ollama está rodando em " + baseUrl + "? Execute 'ollama serve' "
                + "e baixe o modelo com 'ollama pull " + defaultModel() + "'.";
    }

    private static String buildBody(ChatRequest request, String model) {
        return """
                {
                  "model": %s,
                  "messages": [%s],
                  "stream": false,
                  "options": {"temperature": %s, "num_predict": %d}
                }
                """.formatted(JsonUtils.quote(model), toMessagesJson(request),
                request.temperature(), request.maxTokens());
    }

    private static String toMessagesJson(ChatRequest request) {
        return request.messages().stream()
                .map(OllamaClient::toMessageJson)
                .collect(Collectors.joining(", "));
    }

    private static String toMessageJson(ChatMessage message) {
        return "{\"role\": %s, \"content\": %s}".formatted(
                JsonUtils.quote(message.role().wireName()), JsonUtils.quote(message.content()));
    }
}
