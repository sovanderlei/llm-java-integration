package com.exemplo.llm.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Requisição de chat independente de provedor. Crie instâncias via {@link #builder()}.
 *
 * @param model       modelo a usar; {@code null} usa o padrão do cliente
 * @param messages    mensagens da conversa (cópia imutável)
 * @param temperature criatividade, de 0.0 (determinístico) a 2.0
 * @param maxTokens   limite de tokens gerados na resposta
 */
public record ChatRequest(String model, List<ChatMessage> messages, double temperature, int maxTokens) {

    public static final double DEFAULT_TEMPERATURE = 0.7;
    public static final int DEFAULT_MAX_TOKENS = 1024;
    private static final double MAX_TEMPERATURE = 2.0;
    private static final int MAX_TOKENS_LIMIT = 128_000;

    public ChatRequest {
        messages = List.copyOf(Objects.requireNonNull(messages, "messages"));
        validateModel(model);
        validateMessages(messages);
        validateTemperature(temperature);
        validateMaxTokens(maxTokens);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Modelo da requisição ou, se ausente, o {@code fallback}. */
    public String modelOr(String fallback) {
        return model != null ? model : fallback;
    }

    /** Todas as mensagens SYSTEM concatenadas, se houver. */
    public Optional<String> systemPrompt() {
        String joined = messages.stream()
                .filter(m -> m.role() == ChatMessage.Role.SYSTEM)
                .map(ChatMessage::content)
                .collect(Collectors.joining("\n\n"));
        return joined.isEmpty() ? Optional.empty() : Optional.of(joined);
    }

    /** Mensagens USER/ASSISTANT, na ordem original. */
    public List<ChatMessage> conversation() {
        return messages.stream()
                .filter(m -> m.role() != ChatMessage.Role.SYSTEM)
                .toList();
    }

    private static void validateModel(String model) {
        if (model != null && model.isBlank()) {
            throw new IllegalArgumentException("model não pode ser vazio (use null para o padrão)");
        }
    }

    private static void validateMessages(List<ChatMessage> messages) {
        boolean hasUserMessage = messages.stream().anyMatch(m -> m.role() == ChatMessage.Role.USER);
        if (!hasUserMessage) {
            throw new IllegalArgumentException("A requisição precisa de ao menos uma mensagem USER");
        }
    }

    private static void validateTemperature(double temperature) {
        if (Double.isNaN(temperature) || temperature < 0.0 || temperature > MAX_TEMPERATURE) {
            throw new IllegalArgumentException("temperature deve estar entre 0.0 e " + MAX_TEMPERATURE);
        }
    }

    private static void validateMaxTokens(int maxTokens) {
        if (maxTokens < 1 || maxTokens > MAX_TOKENS_LIMIT) {
            throw new IllegalArgumentException("maxTokens deve estar entre 1 e " + MAX_TOKENS_LIMIT);
        }
    }

    /** Builder fluente; as validações acontecem em {@link #build()}. */
    public static final class Builder {

        private final List<ChatMessage> messages = new ArrayList<>();
        private String model;
        private double temperature = DEFAULT_TEMPERATURE;
        private int maxTokens = DEFAULT_MAX_TOKENS;

        private Builder() {
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder system(String content) {
            return message(ChatMessage.system(content));
        }

        public Builder user(String content) {
            return message(ChatMessage.user(content));
        }

        public Builder assistant(String content) {
            return message(ChatMessage.assistant(content));
        }

        public Builder message(ChatMessage message) {
            messages.add(Objects.requireNonNull(message, "message"));
            return this;
        }

        public Builder messages(List<ChatMessage> messages) {
            Objects.requireNonNull(messages, "messages").forEach(this::message);
            return this;
        }

        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public ChatRequest build() {
            return new ChatRequest(model, messages, temperature, maxTokens);
        }
    }
}
