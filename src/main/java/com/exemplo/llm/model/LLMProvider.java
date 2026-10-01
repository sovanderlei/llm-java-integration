package com.exemplo.llm.model;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Provedores de LLM suportados, com URL base, modelo padrão e a variável de
 * ambiente que guarda a API key (quando exigida).
 */
public enum LLMProvider {

    OPENAI("https://api.openai.com/v1", "OPENAI_API_KEY",
            List.of("gpt-4o-mini", "gpt-4o")),

    GEMINI("https://generativelanguage.googleapis.com/v1beta", "GEMINI_API_KEY",
            List.of("gemini-2.0-flash", "gemini-1.5-pro")),

    OLLAMA("http://localhost:11434", null,
            List.of("llama3.2", "qwen2.5-coder", "codellama"));

    private final String baseUrl;
    private final String apiKeyEnvVar;
    private final List<String> knownModels;

    LLMProvider(String baseUrl, String apiKeyEnvVar, List<String> knownModels) {
        this.baseUrl = baseUrl;
        this.apiKeyEnvVar = apiKeyEnvVar;
        this.knownModels = List.copyOf(knownModels);
    }

    public String baseUrl() {
        return baseUrl;
    }

    /** Nome da variável de ambiente com a API key, ou {@code null} se o provedor não exige chave. */
    public String apiKeyEnvVar() {
        return apiKeyEnvVar;
    }

    public boolean requiresApiKey() {
        return apiKeyEnvVar != null;
    }

    /** Modelos conhecidos; o primeiro é o padrão. */
    public List<String> knownModels() {
        return knownModels;
    }

    public String defaultModel() {
        return knownModels.get(0);
    }

    /**
     * Converte um nome (case-insensitive) em provedor.
     *
     * @throws IllegalArgumentException se o nome não corresponder a nenhum provedor
     */
    public static LLMProvider fromName(String name) {
        String normalized = name == null ? "" : name.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(p -> p.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Provedor desconhecido: '" + name + "'. Use um de " + Arrays.toString(values())));
    }
}
