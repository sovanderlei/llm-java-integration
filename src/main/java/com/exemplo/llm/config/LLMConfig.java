package com.exemplo.llm.config;

import com.exemplo.llm.client.GeminiClient;
import com.exemplo.llm.client.LLMClient;
import com.exemplo.llm.client.OllamaClient;
import com.exemplo.llm.client.OpenAIClient;
import com.exemplo.llm.model.LLMProvider;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Fábrica de clientes LLM configurados a partir de variáveis de ambiente.
 *
 * <ul>
 *   <li>{@code OPENAI_API_KEY} / {@code GEMINI_API_KEY} — obrigatórias para cada provedor;</li>
 *   <li>{@code LLM_MODEL} — opcional, sobrescreve o modelo padrão;</li>
 *   <li>{@code OLLAMA_BASE_URL} — opcional, padrão {@code http://localhost:11434}.</li>
 * </ul>
 */
public final class LLMConfig {

    public static final String MODEL_ENV = "LLM_MODEL";
    public static final String OLLAMA_URL_ENV = "OLLAMA_BASE_URL";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

    private LLMConfig() {
    }

    /**
     * Cria o cliente do provedor informado.
     *
     * @throws IllegalStateException se a API key exigida não estiver definida
     */
    public static LLMClient createClient(LLMProvider provider) {
        Objects.requireNonNull(provider, "provider");
        String model = env(MODEL_ENV).orElse(provider.defaultModel());
        return switch (provider) {
            case OPENAI -> new OpenAIClient(httpClient(HttpClient.Version.HTTP_2), requireApiKey(provider), model);
            case GEMINI -> new GeminiClient(httpClient(HttpClient.Version.HTTP_2), requireApiKey(provider), model);
            // Ollama é HTTP puro local: HTTP/1.1 evita a tentativa de upgrade para h2c.
            case OLLAMA -> new OllamaClient(httpClient(HttpClient.Version.HTTP_1_1), ollamaBaseUrl(), model);
        };
    }

    private static HttpClient httpClient(HttpClient.Version version) {
        return HttpClient.newBuilder()
                .version(version)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    private static String requireApiKey(LLMProvider provider) {
        String variable = provider.apiKeyEnvVar();
        return env(variable).orElseThrow(() -> new IllegalStateException("""
                Variável de ambiente %1$s não definida (exigida por %2$s).
                  bash/zsh:   export %1$s="sua-chave"
                  PowerShell: $env:%1$s="sua-chave"
                Ou use o Ollama local, que não exige chave.""".formatted(variable, provider)));
    }

    private static URI ollamaBaseUrl() {
        String url = env(OLLAMA_URL_ENV).orElse(LLMProvider.OLLAMA.baseUrl());
        return URI.create(url.contains("://") ? url : "http://" + url);
    }

    private static Optional<String> env(String name) {
        return Optional.ofNullable(System.getenv(name))
                .map(String::trim)
                .filter(value -> !value.isEmpty());
    }
}
