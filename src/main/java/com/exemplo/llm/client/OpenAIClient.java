package com.exemplo.llm.client;

import com.exemplo.llm.model.ChatMessage;
import com.exemplo.llm.model.ChatRequest;
import com.exemplo.llm.model.LLMProvider;
import com.exemplo.llm.util.JsonUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Cliente da API Chat Completions da OpenAI.
 *
 * <p>Peculiaridades: roles {@code system|user|assistant} no array {@code messages[]},
 * autenticação via {@code Authorization: Bearer} e resposta em
 * {@code choices[0].message.content}.
 */
public final class OpenAIClient extends AbstractLLMClient {

    private final URI endpoint;
    private final String apiKey;

    public OpenAIClient(HttpClient httpClient, String apiKey, String defaultModel) {
        super(httpClient, defaultModel);
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
        this.endpoint = URI.create(LLMProvider.OPENAI.baseUrl() + "/chat/completions");
    }

    @Override
    public LLMProvider provider() {
        return LLMProvider.OPENAI;
    }

    @Override
    protected HttpRequest buildHttpRequest(ChatRequest request, String model) {
        return jsonPost(endpoint, buildBody(request, model))
                .header("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    protected String extractContent(String responseBody) {
        return requireContent(JsonUtils.findString(responseBody, "content", "choices"), responseBody);
    }

    private static String buildBody(ChatRequest request, String model) {
        return """
                {
                  "model": %s,
                  "messages": [%s],
                  "temperature": %s,
                  "max_tokens": %d
                }
                """.formatted(JsonUtils.quote(model), toMessagesJson(request),
                request.temperature(), request.maxTokens());
    }

    private static String toMessagesJson(ChatRequest request) {
        return request.messages().stream()
                .map(OpenAIClient::toMessageJson)
                .collect(Collectors.joining(", "));
    }

    private static String toMessageJson(ChatMessage message) {
        return "{\"role\": %s, \"content\": %s}".formatted(
                JsonUtils.quote(message.role().wireName()), JsonUtils.quote(message.content()));
    }
}
